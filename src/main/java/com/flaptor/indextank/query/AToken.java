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

package com.flaptor.indextank.query;


public abstract class AToken {
    public abstract String getText();
    public abstract int getPosition();
    public abstract int getStartOffset();
    public abstract int getEndOffset();
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer("token(text: ");
        buff.append(getText())
            .append(", position: ")
            .append(getPosition())
            .append(", startOffset: ")
            .append(getStartOffset())
            .append(", endOffset: ")
            .append(getEndOffset());
        return buff.toString();
    }
}
