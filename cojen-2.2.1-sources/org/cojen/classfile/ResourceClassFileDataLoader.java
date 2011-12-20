/*
 *  Copyright 2004-2010 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cojen.classfile;

import java.io.InputStream;

/**
 * {@link ClassFileDataLoader} implementation that loads class file data as
 * resources from a ClassLoader.
 *
 * @author Brian S O'Neill
 */
public class ResourceClassFileDataLoader implements ClassFileDataLoader {
    private final ClassLoader mLoader;

    /**
     * Loads resources using the ClassLoader that loaded this class.
     */
    public ResourceClassFileDataLoader() {
        mLoader = getClass().getClassLoader();
    }

    /**
     * @param loader ClassLoader for finding resources; if null, use system
     * ClassLoader.
     */
    public ResourceClassFileDataLoader(ClassLoader loader) {
        mLoader = loader;
    }

    public InputStream getClassData(String name) {
        name = name.replace('.', '/') + ".class";

        InputStream in;
        if (mLoader == null) {
            in = ClassLoader.getSystemResourceAsStream(name);
        } else {
            in = mLoader.getResourceAsStream(name);
        }

        return in;
    }
}
