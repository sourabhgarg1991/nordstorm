package org;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class SampleCode2 {
//Overloading
        // 1. Method to add two integers
        public int add(int a, int b) {
            return a + b;
        }

        // 2. Overloaded method: different number of arguments
        public int add(int a, int b, int c) {
            return a + b + c;
        }

    // 2. Overloaded method: different number of arguments
    public String add(String a, String b) {
        return a + b; //this will concat the string and return.
    }

    public static void main(String[] args){
            SampleCode2 obj = new SampleCode2();
            System.out.println(obj.add(1, 2));
            System.out.println(obj.add(1, 2, 3));
        System.out.println(obj.add("Hello ", "World"));
    }
}































