# JVM
JVM调优基础
## JVM总体架构
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