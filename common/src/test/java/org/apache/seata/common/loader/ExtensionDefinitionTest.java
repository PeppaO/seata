/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.common.loader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Extension definition test.
 */
public class ExtensionDefinitionTest {

    @Test
    public void testEquals() {
        ExtensionDefinition<ChineseHello> definition
                = new ExtensionDefinition<>("abc", 1, Scope.PROTOTYPE, ChineseHello.class);
        ExtensionDefinition<ChineseHello> definition2
                = new ExtensionDefinition<>("abc", 1, Scope.PROTOTYPE, ChineseHello.class);
        Assertions.assertEquals(definition2, definition);
    }

    @Test
    public void testConstructorAndGetters() {
        ExtensionDefinition<ChineseHello> definition =
                new ExtensionDefinition<>("abc", 1, Scope.PROTOTYPE, ChineseHello.class);
        Assertions.assertEquals("abc", definition.getName());
        Assertions.assertEquals(1, definition.getOrder());
        Assertions.assertEquals(Scope.PROTOTYPE, definition.getScope());
        Assertions.assertEquals(ChineseHello.class, definition.getServiceClass());
    }

    @Test
    public void testHashCode() {
        ExtensionDefinition<ChineseHello> definition =
                new ExtensionDefinition<>("abc", 1, Scope.PROTOTYPE, ChineseHello.class);
        ExtensionDefinition<ChineseHello> definition2 =
                new ExtensionDefinition<>("abc", 1, Scope.PROTOTYPE, ChineseHello.class);
        Assertions.assertEquals(definition.hashCode(), definition2.hashCode());
    }
}
