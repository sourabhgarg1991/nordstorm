package org;

public class Factory {
    public static void main(String[] args){
        Car car = new Odyssey();
        Car newCar = new Sportage();

        System.out.println(newCar.color);
    }
}
