package cn.melon;

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
