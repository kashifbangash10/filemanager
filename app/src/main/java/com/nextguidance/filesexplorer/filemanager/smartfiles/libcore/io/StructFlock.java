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

package com.nextguidance.filesexplorer.filemanager.smartfiles.libcore.io;

/**
 * Information returned/taken by fcntl(2) F_GETFL and F_SETFL. Corresponds to C's
 * {@code struct flock} from
 * <a href="http:
 */
public final class StructFlock {
    
    public short l_type;

    
    public short l_whence;

    
    public long l_start; 

    
    public long l_len; 

    
    public int l_pid; 
}
