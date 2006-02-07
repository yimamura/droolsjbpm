package org.drools.util.asm;

import java.lang.reflect.Method;

import junit.framework.TestCase;

public class ClassFieldInspectorTest extends TestCase {

    public void testIt() throws Exception {
        ClassFieldInspector ext = new ClassFieldInspector( Person.class );
        assertEquals(3, ext.getPropertyGetters().size());
        assertEquals("getAge", ((Method) ext.getPropertyGetters().get(0)).getName());
        assertEquals("isHappy", ((Method) ext.getPropertyGetters().get(1)).getName());
        assertEquals("getName", ((Method) ext.getPropertyGetters().get(2)).getName());
    }
    
    static class Person {
        private boolean happy;
        private String name;
        private int age;
        public int getAge() {
            return age;
        }
        public void setAge(int age) {
            this.age = age;
        }
        public boolean isHappy() {
            return happy;
        }
        public void setHappy(boolean happy) {
            this.happy = happy;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        
        private boolean isBogus() {
            return false;
        }
        
        //this will not show up as it is a getter that takes an argument
        public String getAlsoBad(String s) {
            return "ignored";
        }
        
        
        
    }
    
}
