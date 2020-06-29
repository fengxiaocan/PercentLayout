/*
 * Copyright (C) 2019 Cricin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.app.perlint.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.ApiKt;
import com.android.tools.lint.detector.api.Issue;
import com.app.perlint.dom.PercentDomExtender;

import java.util.Collections;
import java.util.List;

/**
 * IssueRegistry：用于注册要检查的Issue列表。自定义Lint需要生成一个jar文件，其Manifest指向IssueRegistry类。
 */
@SuppressWarnings("unused")
public final class PercentIssueRegistry extends IssueRegistry{


    static{
        installStudioPlugins();
    }

    private static void installStudioPlugins(){
        boolean lintInsideStudio;
        try{
            Class.forName("org.jetbrains.android.dom.AndroidDomElement");
            lintInsideStudio=true;
        } catch(ClassNotFoundException e){
            lintInsideStudio=false;
        }
        if(lintInsideStudio){
            try{
                PercentDomExtender.install();
            } catch(Exception ignore){
            }
        }
    }

    @Override
    public List<Issue> getIssues(){
        return Collections.emptyList();
    }

    @Override
    public int getApi(){
        return ApiKt.CURRENT_API;
    }
}
