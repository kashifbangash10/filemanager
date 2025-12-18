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
 * Corresponds to C's {@code struct linger} from
 * <a href="http:
 */
public final class StructLinger {
    
    public final int l_onoff;

    
    public final int l_linger;

    public StructLinger(int l_onoff, int l_linger) {
        this.l_onoff = l_onoff;
        this.l_linger = l_linger;
    }

    public boolean isOn() {
        return l_onoff != 0;
    }

    @Override public String toString() {
        return "StructLinger[l_onoff=" + l_onoff + ",l_linger=" + l_linger + "]";
    }
}
