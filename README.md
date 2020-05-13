# JVM
JVM调优基础
## JVM总体架构（jdk1.7及之前）
> 三个主要子系统
1. 类加载器子系统
2. 运行时数据区（核心：栈、堆、方法区）
3. 执行引擎
![JVM 图片](https://tse3-mm.cn.bing.net/th/id/OIP.BD5VTP9JSU5mPURmJfx8BAAAAA?pid=Api&rs=1)

> 虚拟机栈(Java Virtual Machine Stacks)

![VMStack 图片](https://uploadfiles.nowcoder.com/files/20190111/7380095_1547140255170_4685968-a6b55efe15a7a9ed.png)  
* 每创建一个线程时就会对应创建一个Java栈，所以Java栈也是"线程私有"的内存区域
* 每调用一个方法时就会往栈中创建并压入一个栈帧，栈帧是用来<span style="color=red">存储方法数据和部分过程结果的数据结构</span>
* 每一个方法从调用到最终返回结果的过程，就对应一个栈帧从入栈到出栈的过程
* Current Frame --- 当前活动帧

> 方法区(Method Area 存储类结构信息)

![MethodArea 图片](https://tse1-mm.cn.bing.net/th/id/OIP.CUdr1UG9vbYt5MZN0_7QOQHaFI?pid=Api&rs=1)
* 常量池 --- 包含一些常量和符号引用（加载类的连接阶段中的解析过程会将符号引用转换为直接引用）
    * 字符串也是存放在方法区的常量池中
* 方法区是线程共享的

> 堆(heap 存储Java实例或对象)

![heap 图片](https://tse1-mm.cn.bing.net/th/id/OIP.U8wg7nxs3yBHzSBH8whM0QHaC-?pid=Api&rs=1)
* 堆是GC（垃圾回收的主要区域）
* 堆也是线程共享的

先看看一种垃圾回收算法（分代算法）
1. 新建的对象会放在新生代（eden），eden存满时触发minor gc
2. minor gc会清除包括s0，s1在内的所有新生代垃圾
3. eden内没被清除的对象进入幸存区，被放如s0/s1中（空的那一个），年龄为1
4. 幸存区幸存的对象移入空的幸存区，年龄+1，年龄达到某值时，进入老年代
5. 老年代存满后，触发major gc / full gc
6. major gc仅回收老年代垃圾；触发full gc会出现STW（stop the world），回收全部垃圾  

各代内存的大小关系
1. eden 和 survivor的比例为8：1：1
2. 老年代比新生代内存大（因为新生代对象大部分都是朝生夕死的）

JVM判定是否是垃圾数据的一般方法：
1. 大多数垃圾收集器都使用  引用的根集---GC root来分析对象是否存活
2. 引用的根集是正在执行的Java程序随时都可以访问引用的变量的集合
3. 从这些根集变量出发可直接或间接访问的对象即存活对象
4. 无法到达的对象就会成为下一次垃圾回收的对象

> 例1：Demo1.java  
```
    public class Demo1 {
        static Integer i1 = new Integer(1);
        static Integer i2 = new Integer(1);
    
        static Integer a = 1;
        static Integer b = 1;
    
        public static void main(String[] args) {
            System.out.println(i1==i2);
            System.out.println(a==b);
        }
    }
```
* 结果：false true
* 执行过程：
    + 执行main之前，创建一个主线程
    + 当首次访问类的静态方法或静态属性时 --- 类加载器加载在类路径上的Demo.class文件
    + 加载完Demo.class后，有关静态初始化的所有动作都会被执行 --- 初始化i1、i2、a、b
    + 初始化i1，i2时访问构造器，构造器也是一种静态方法，所以加载Integer.class
    + 在堆中存放i1、i2对象，栈中变量表内有i1、i2（指向堆中相应的对象）
    + 在方法区的常量池中存放常量1（只有一个），栈中变量表中有a、b（指向同一个1）

## 有关JVM内存的参数配置  
![jvm内存配置 图片](https://img-blog.csdnimg.cn/20190219111109518.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3hpYW9saW5udWxpZHVzaHU=,size_16,color_FFFFFF,t_70)

#### -Xms
> JVM 启动时申请的初始Heap值（默认为物理内存的1/64但小于1G）  
> （默认当剩余堆内存大于70%时JVM会减小heap的大小到-Xms指定的大小）  
> （可通过-XX:MaxHeapFreeRation=来指定这个比例）

#### -Xmx
> JVM可以申请的最大Heap值，（默认值为物理内存的1/4但小于1G）  
> （默认当剩余堆内存小于40%时，JVM会增大Heap到-Xmx指定的大小）  
> （可通过-XX:MinHeapFreeRation=来指定这个比例）

* 最好将 -Xms 和 -Xmx设为相同值，避免每次垃圾回收完成后JVM重新分配内存，也可以减少垃圾回收次数

#### -Xmn
> 设置新生代的大小  
> 对于HotSpot类型的虚拟机来说（堆大小 = 新生代 + 老年代 + 持久代）（持久代一般固定为64M）  
> 增加新生代大小，将会减少老年代大小，该设置对系统性能影响较大

* 有两种情况对象可直接进入老年代
    * 大对象：-XX:PretenureSizeThreshold=1024(默认为0)
        * 即大于1024个字节的对象可直接进入老年代
    * 大的数组对象，且数组中无引用外部对象

#### -Xss
> 设置Java每个线程的Stack大小（默认为1M）  
> 设太大，则最大线程数变少（推荐256K）

#### -XX:PremSize -XX:MaxPremSize (持久代大小，最好设为同一值)
#### -XX:+MaxTenuringThreshold=10 (垃圾的最大年龄)

##内存溢出和内存泄漏
> 内存泄漏 ---- memory leak  
* 值在程序申请内存后，无法释放已申请的内存空间
1. 常发性内存泄漏
    * 发生内存泄漏的代码会被多次执行，每次都发生内存泄漏
2. 偶发性内存泄漏
    * 在特定的环境下，偶发性也许会变成常发性
3. 一次性内存泄漏
    * 比如类的构造函数内
4. 隐式内存泄漏
    * 这个虽然没有发生内存泄漏，但因为程序运行过程中不断地分配内存，直到结束在释放

> 内存溢出 ---- out of memory  OOM
* 指程序申请内存时，没有足够地内存供申请使用
1. 内存泄漏地堆积会导致内存溢出
2. 内存中加载地数据量过于庞大
3. 集合类中有对对象的引用，使用完后未清空，使JVM不能回收
4. 代码存在死循环或循环中产生过多对象实体
5. 启动参数内存值设定过小

* 解决方案：
1. 手动修改JVM启动参数
2. 检查错误日志，看OOM之前是否出现其他错误异常
3. 对代码进行走查和分析，找出可能发生内存溢出的位置

## 垃圾回收机制以及核心参数配置
> 垃圾回收机制是什么  
1. 自动垃圾收集机制是不定时查看堆内存、判定哪些对象可达/不可达、删除不可达对象的一个过程

> 垃圾回收算法  
1. 引用计数法（废弃）
    * 给对象添加一个引用计数器，每一个地方引用他时，计数器加1
    1. 两个对象相互引用时，gc永远都清除不了
    2. 无法解决 多种**引用类型** 的问题  
2. 复制算法
    * 将内存分为大小相等的两部分，每次只使用其中一部分
    * 一部分用完了就垃圾回收，将活下来的对象，复制到另一部分
    1. 简单、高效、无碎片
    2. 但导致有50%的内存空间始终处于空闲状态
3. 标记清除算法
    * 标记：标记使用/未使用的对象（或者说内存块）
    * 清除：清除无引用的对象
    * 维护指向空闲空间的指针链表，（请求时从中计算出合适的内存块分配）
4. 标记压缩算法
    * 标记
    * 带压缩删除 --- 将存活对象移动到一起
    1. 消除碎片
    2. 不需要维护指针链表，计算量小，更快速
5. 分代算法
    * 新生代 ： 复制算法
    * 老年代： 标记清除 / 标记压缩算法
> 引用类型  
> （程序员可通过代码决定对象的生命周期） 
1. Java对象的引用类型（四种：
    * 强引用 --- 正常引用就是强引用
    * 软引用 --- 使用SoftReference包装
        * 可用来实现内存敏感的高速缓存
        * 垃圾回收线程会在虚拟机抛出OOM之前回收软可及对象（按照闲置时间）
        * 回收前，get返回强引用，回收后返回null
    * 弱引用 --- 使用WeakReference包装
        * 软引用也是用来描述非必须对象的，当JVM进行垃圾回收时，无论内存是否充足，都会回收弱引用关联的对象
        * 如果弱引用关联对象还存在强引用，则垃圾回收器不会回收该对象
    * 虚引用（使用非常少）
        * 虚引用关联跟没有引用与之关联一样
        * 所以有什么用途呢？

> 垃圾回收器
1. Serial回收器 --- 串行回收器
    * 稳定，效率高，但可能产生较长时间的停顿
    * 只使用一个线程回收
    * 新生代复制算法、老年代标记压缩清除
    * 垃圾回收过程会暂停服务
    + **-XX:+UseSerialGC**
2. ParNew回收器 --- 是Serial的多线程版本
    * 新生代并行
    * 老年代串行
    * **-XX:+UserParNewGC**
    * **-XX:ParallelGCThreads** 限制线程数量
3. Parallel回收器 --- 更关注系统**吞吐量**
    * 新生代并行
    * 老年代串行
    * 可通过参数打开**自适应调节策略** （提供合适停顿时间）
    * **-XX:+UseParallelGC**
4. Parallel Old回收器
    * 老年代并行
    * **-XX:+UseParallelOldGC**
5. CMS回收器 --- 停顿时间短
    * Concurrent Mark Sweep --- 提供最短回收停顿时间
    * 初始标记 --- STW
    * 并发标记
    * 重新标记 --- STW
    * 并发清除
    * CMS是老年代回收器（新生代使用ParNew）
    * 基于标记-清除
    1. 产生大量空间碎片，并发阶段会降低吞吐量
    * **-XX:+UseConcMarkSweepGC**
    * **-XX:+UseCMSCompactAtFullCollection** full gc后，进行碎片整理
    * **-XX:+CMSFullGCsBeforeCompaction** 设置进行几个full gc才进行碎片整理
    * **-XX:ParallelCMSThread**
    * **-XX:CMSInitiatingOccupancyFraction=80** 当老年代使用空间达到80%时，进行full gc
6. G1回收器
    * 目前技术发展最前沿成果之一
    1. 空间整合 --- 标记-压缩（整理）
    2. 可预测停顿 --- 实时垃圾回收


## JVM升级 和 配置 总结
> jdk1.8 采用HotSpot 和 JRockit两个JVM中的精华  
1. Sun公司的HotSpot ---- jdk1.7及以前
2. BEA公司的JRockit
3. IBM公司的J9 JVM

> jdk1.8  
1. 永恒代被移除（也就是方法区）
    * jdk1.7之前，Java类信息，常量池，静态变量都存储在Perm（永恒代）
    * jdk1.8，将类元数据放到本地内存中，将常量池、静态变量放到Java堆里
2. -XX:PermSize 和 -XX:MaxPermSize 失效
3. -XX:MetaSpaceSize 和 -XX:MaxMetaSpaceSize 替代
> 改进之后的优点  
1. 类元信息现在可以使用更多本地内存，一定程度上减少了原来运行时生成大量类（反射、代理）经常造成FUll GC
2. 各个项目会共享同样的class内存空间

> JVM启动参数分类  
1. 标准参数（-）
    * 所有JVM实现都必须实现这些参数的功能
2. 非标准参数（-X）
    * 不保证所有JVM实现都满足，不保证向后兼容
3. 非Stable参数（-XX）
    * 各个JVM实现会有所不同
    * 将来可能会随时取消
    
> JVM参数配置的经验  
1. 年轻代和老年代的大小
    * 将对象类型分为三种：
        1. 可能一直活着的对象
        2. 活的很短的对象
        3. 介于1，2之间的对象
    * 如果2类型对象特别多，可以将新生代设置大一点，这样每次GC时，很多对象已经死亡
    * 大总没有问题，唯一的问题就是可能会浪费空间
    * 要在有限的内存空间分配好新生代和老年代的比例才是问题的关键
2. 碎片问题
    * 老年代的并发收集器使用标记-清除算法，不会堆堆进行压缩
    * 设置开启Full GC后对老年代进行压缩
3. 深入后再补充

> 实战，下面配置能否看懂  
```
jdk 1.7 生产虚拟机参数（添加到 catalina.sh中）
JAVA_OPTS=-Xmx8000M -Xms8000M -Xmn1024M -XX:PermSize=2048M -XX:MaxPermSize=2048M -Xss256K 
-XX:+DisableExplicitGC -XX:SurvivorRatio=1 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC 
-XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:CMSFullGCsBeforeCompaction=0 
-XX:+CMSClassUnloadingEnabled -XX:LargePageSizeInBytes=128M -XX:+UseFastAccessorMethods 
-XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=80 -XX:SoftRefLRUPolicyMSPerMB=0 
-XX:+PrintClassHistogram -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC 
-Xloggc:/tmp/gc.log
```

```
jdk 1.8 （注意新版本metaspace 代替了旧版本PermGen space）
JAVA_OPTS=-server -Xmx8g -Xms8g -Xmn2g  -XX:MetaspaceSize=2g -XX:MaxMetaspaceSize=2g -Xss256k 
-XX:+DisableExplicitGC  -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled 
-XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly 
-XX:CMSInitiatingOccupancyFraction=70 -Duser.timezone=GMT+8 -XX:+PrintClassHistogram 
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps  -XX:+PrintGCDateStamps   -XX:+PrintHeapAtGC  
-XX:+HeapDumpOnOutOfMemoryError  -XX:HeapDumpPath=/tmp/b2b_interface.dump   
-Xloggc:/tmp/b2b_interface_gc.log -verbose:gc -Xmixed -XX:-CITime
```

> Tomcat调优  
* 使用JMeter进行压力测试
