package org.drools.base;

/*
 * Copyright 2005 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.drools.RuntimeDroolsException;
import org.drools.spi.ObjectType;

/**
 * Java class semantics <code>ObjectType</code>.
 * 
 * @author <a href="mailto:bob@werken.com">bob@werken.com </a>
 * 
 * @version $Id: ClassObjectType.java,v 1.5 2005/02/04 02:13:36 mproctor Exp $
 */
public class ClassObjectType
    implements
    ObjectType {
    // ------------------------------------------------------------
    // Instance members
    // ------------------------------------------------------------

    /**
     * 
     */
    private static final long serialVersionUID = 320;

    /** Java object class. */
    protected Class           objectTypeClass;

    protected ValueType       valueType;

    protected boolean         shadowEnabled;

    protected Class           shadowClass;

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------

    /**
     * Creates a new class object type with shadow disabled.
     * 
     * @param objectTypeClass
     *            Java object class.
     */
    public ClassObjectType(final Class objectTypeClass) {
        this( objectTypeClass,
              null );
    }

    /**
     * Creates a new class object type with the given class
     * as its shadow class.
     * 
     * @param objectTypeClass
     *            Java object class.
     * @param shadowClass
     *            The shadow class for the given objectTypeClass
     */
    public ClassObjectType(final Class objectTypeClass,
                           final Class shadowClass) {
        this.objectTypeClass = objectTypeClass;
        this.valueType = ValueType.determineValueType( objectTypeClass );
        if ( shadowClass != null ) {
            this.shadowClass = shadowClass;
            this.shadowEnabled = true;
        }
    }

    // ------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------

    /**
     * Return the Java object class.
     * 
     * @return The Java object class.
     */
    public Class getClassType() {
        return this.objectTypeClass;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // org.drools.spi.ObjectType
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * Determine if the passed <code>Object</code> belongs to the object type
     * defined by this <code>objectType</code> instance.
     * 
     * @param object
     *            The <code>Object</code> to test.
     * 
     * @return <code>true</code> if the <code>Object</code> matches this
     *         object type, else <code>false</code>.
     */
    public boolean matches(final Object object) {
        return getClassType().isInstance( object );
    }

    public ValueType getValueType() {
        return this.valueType;
    }

    public Object getShadow(final Object fact) throws RuntimeDroolsException {
        ShadowProxy proxy = null;
        if ( isShadowEnabled() ) {
            try {
                proxy = (ShadowProxy) this.shadowClass.getConstructor( new Class[]{this.objectTypeClass} ).newInstance( new Object[]{fact} );
            } catch ( final Exception e ) {
                throw new RuntimeDroolsException( "Error creating shadow fact for object: " + fact,
                                                  e );
            }
        }
        return proxy;
    }

    public boolean isShadowEnabled() {
        return this.shadowEnabled;
    }

    public String toString() {
        return "[ClassObjectType class=" + getClassType().getName() + "]";
    }

    /**
     * Determine if another object is equal to this.
     * 
     * @param object
     *            The object to test.
     * 
     * @return <code>true</code> if <code>object</code> is equal to this,
     *         otherwise <code>false</code>.
     */
    public boolean equals(final Object object) {
        if ( this == object ) {
            return true;
        }

        if ( object == null || object.getClass() != ClassObjectType.class ) {
            return false;
        }

        return this.objectTypeClass == ((ClassObjectType) object).objectTypeClass;
    }

    public int hashCode() {
        return this.objectTypeClass.hashCode();
    }

}