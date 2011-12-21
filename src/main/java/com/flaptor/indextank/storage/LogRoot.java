/*
 * Copyright (c) 2011 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.flaptor.indextank.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.ConcurrentMap;

import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.google.common.collect.MapMaker;

public class LogRoot {
    
    public static final File DEFAULT_PATH = new File("/data/storage");
    public static final int DEFAULT_READING_PAGE_SIZE = 5 * 1024 * 1024;
    
    private ConcurrentMap<String, FileChannel> components = new MapMaker().makeMap();
    private File path;

    private int readingPageSize;
    
    public LogRoot() {
        this(DEFAULT_PATH, DEFAULT_READING_PAGE_SIZE);
    }

    public LogRoot(File rootPath, int readingPageSize) {
        this.path = rootPath;
        this.readingPageSize = readingPageSize;
    }
    
    public File getPath() {
        return path;
    }
    
    public File getRawLogPath() {
        return new File(getPath(), "raw");
    }
    
    public File getHistoryLogPath() {
        return new File(getRawLogPath(), "history");
    }
    
    public File getLiveLogPath() {
        return new File(getRawLogPath(), "live");
    }
    
    public File getIndexesLogPath() {
        return new File(getPath(), "indexes");
    }

    public File getPreviousPath() {
        return new File(getPath(), "previous");
    }

    public File getPreviousIndexesLogPath() {
        return new File(getPreviousPath(), "indexes");
    }
    
    public File getIndexLogPath(String code) {
        return new File(getIndexesLogPath(), code);
    }
    
    public File getSafeToReadFile() {
        return new File(getPath(), "safe_to_read");
    }
    
    public File getMigratedPath() {
        return new File(getPath(), "migrated");
    }
    public File getMigratedIndexesLogPath() {
        return new File(getMigratedPath(), "indexes");
    }
    
    public boolean lockComponent(String name) throws FileNotFoundException {
        File file = lockFile(name);
        RandomAccessFile raf = new RandomAccessFile(file, "rwd");
        FileChannel channel = raf.getChannel();
        try {
            FileLock l = channel.tryLock();
            if (l == null) {
                Execute.close(channel);
                return false;
            }
            file.deleteOnExit();
            components.put(name, channel);
            return true;
        } catch (Exception e) {
            Execute.close(channel);
            return false;
        }
    }

    public boolean unlockComponent(String name) {
        FileChannel channel = components.remove(name);
        if (channel == null) {
            return false;
        }
        Execute.close(channel);
        return true;
    }
    
    public String loadInfo(String name) throws IOException {
        File file = infoFile(name);
        return file.exists() ? FileUtil.readFile(file).trim() : null;
    }

    public void saveInfo(String name, String info) throws IOException {
        FileUtil.writeFile(infoFile(name), info);
    }
    
    private File infoFile(String name) {
        return new File(getPath(), name + ".info");
    }
    
    private File lockFile(String name) {
        return new File(getPath(), name + ".lock");
    }
    
    public int getReadingPageSize() {
        return readingPageSize;
    }

    
    
    
}
