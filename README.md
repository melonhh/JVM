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

> 例1：Demo1.java
        
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
* 结果：false true
* 执行过程：
    + 执行main之前，创建一个主线程
    + 当首次访问类的静态方法或静态属性时 --- 类加载器加载在类路径上的Demo.class文件
    + 加载完Demo.class后，有关静态初始化的所有动作都会被执行 --- 初始化i1、i2、a、b
    + 初始化i1，i2时访问构造器，构造器也是一种静态方法，所以加载Integer.class
    + 在堆中存放i1、i2对象，栈中变量表内有i1、i2（指向堆中相应的对象）
    + 在方法区的常量池中存放常量1（只有一个），栈中变量表中有a、b（指向同一个1）
