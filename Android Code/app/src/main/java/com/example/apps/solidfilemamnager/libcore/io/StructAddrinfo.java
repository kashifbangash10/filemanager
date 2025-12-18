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

import java.net.InetAddress;

/**
 * Information returned/taken by getaddrinfo(3). Corresponds to C's {@code struct addrinfo} from
 * <a href="http:
 *
 * TODO: we currently only _take_ a StructAddrinfo; getaddrinfo returns an InetAddress[].
 */
public final class StructAddrinfo {
    
    public int ai_flags;

    
    public int ai_family;

    
    public int ai_socktype;

    
    public int ai_protocol;

    


    
    public InetAddress ai_addr;

    


    
    public StructAddrinfo ai_next;
}
