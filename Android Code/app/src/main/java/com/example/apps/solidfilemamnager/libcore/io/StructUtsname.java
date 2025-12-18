/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.apps.solidfilemamnager.libcore.io;

/**
 * Information returned by uname(2). Corresponds to C's
 * {@code struct utsname} from
 * <a href="http:
 */
public final class StructUtsname {
    
    public final String sysname;

    
    public final String nodename;

    
    public final String release;

    
    public final String version;

    
    public final String machine;

    StructUtsname(String sysname, String nodename, String release, String version, String machine) {
        this.sysname = sysname;
        this.nodename = nodename;
        this.release = release;
        this.version = version;
        this.machine = machine;
    }
}
