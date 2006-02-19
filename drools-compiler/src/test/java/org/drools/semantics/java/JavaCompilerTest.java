package org.drools.semantics.java;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import org.drools.CheckedDroolsException;
import org.drools.base.ClassFieldExtractor;
import org.drools.lang.descr.AndDescr;
import org.drools.lang.descr.ColumnDescr;
import org.drools.lang.descr.FieldBindingDescr;
import org.drools.lang.descr.PackageDescr;
import org.drools.lang.descr.PredicateDescr;
import org.drools.lang.descr.ReturnValueDescr;
import org.drools.lang.descr.RuleDescr;
import org.drools.rule.Rule;
import org.drools.rule.Package;
import org.drools.util.asm.FieldAccessor;
import org.drools.util.asm.FieldAccessorGenerator;
import org.drools.util.asm.FieldAccessorMap;

import junit.framework.TestCase;

public class JavaCompilerTest extends TestCase {

    public void testReturnValue() throws Exception {
        DdjCompiler compiler = new DdjCompiler();
        
        PackageDescr packageDescr = new PackageDescr("p1");        
        RuleDescr ruleDescr = new RuleDescr("rule-1");
        packageDescr.addRule( ruleDescr );
        
        AndDescr lhs = new AndDescr();
        ruleDescr.setLhs( lhs );
        
        ColumnDescr column = new ColumnDescr(Cheese.class.getName());
        lhs.addDescr( column );
        
        FieldBindingDescr fieldBindingDescr = new FieldBindingDescr("price", "x");
        column.addDescr( fieldBindingDescr );
        fieldBindingDescr = new FieldBindingDescr("type", "y");
        column.addDescr( fieldBindingDescr );
        
        packageDescr.addGlobal( "p", "java.util.Map" );                  
        
        ReturnValueDescr returnValue = new ReturnValueDescr("price", "==", " y == p.get(x) * 2");
        column.addDescr( returnValue );                                        
        
        compiler.addPackage( packageDescr );
    }
    
    public void testPredicate() throws CheckedDroolsException, ClassNotFoundException {
        DdjCompiler compiler = new DdjCompiler();
        
        PackageDescr packageDescr = new PackageDescr("p1");        
        RuleDescr ruleDescr = new RuleDescr("rule-1");
        packageDescr.addRule( ruleDescr );
        
        AndDescr lhs = new AndDescr();
        ruleDescr.setLhs( lhs );
        
        ColumnDescr column = new ColumnDescr(Cheese.class.getName());
        lhs.addDescr( column );
        
        FieldBindingDescr fieldBindingDescr = new FieldBindingDescr("price", "x");
        column.addDescr( fieldBindingDescr );
        fieldBindingDescr = new FieldBindingDescr("type", "y");
        column.addDescr( fieldBindingDescr );

        fieldBindingDescr = new FieldBindingDescr("price", "z");
        column.addDescr( fieldBindingDescr );        
        
        packageDescr.addGlobal( "p", "java.util.Map" );                  
        
        PredicateDescr predicate = new PredicateDescr("price", "z", " y == p.get(x) * z");
        column.addDescr( predicate );                                        
        
        compiler.addPackage( packageDescr );
    }    
    
    private Class getClassType(Class clazz,
                               String name) throws IntrospectionException {
        Class fieldType = null;
        PropertyDescriptor[] descriptors = Introspector.getBeanInfo( clazz ).getPropertyDescriptors();
        for ( int i = 0; i < descriptors.length; i++ ) {
            if ( descriptors[i].getName().equals( name ) ) {
                fieldType = descriptors[i].getPropertyType();
                break;
            }
        }

        return fieldType;
    }    
    
    public class Cheese {
        private String type;
        private int price;
        public Cheese(String type,
                      int price) {
            super();
            this.type = type;
            this.price = price;
        }
        public int getPrice() {
            return price;
        }       
        
        public String getType() {
            return type;
        }                        
    }
}
