#!groovy

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
def call(Map params = [:]) {
    pipeline {
        agent { node { label 'ubuntu' } }
        stages {
            stage("Netbeans ICLA Info") {
                when { changeRequest() }
                steps {
                    sh 'printenv'
                    script {
                        // GitHub requires new lines in comments to be windows style
                        String NL = "\r\n";
                   
                        // Mark the comments we create
                        String MARKER_COMMENT = "<!-- Autocomment Netbeans ICLA Info Job-->"
                        
                        //Message start
                        String message = MARKER_COMMENT + NL
                                        
                        message += "Netbeans ICLA Info" + NL
                        
                        withCredentials([usernamePassword(credentialsId: 'ASF_Cloudbees_Jenkins_ci-builds',
                                          usernameVariable: 'GITHUB_APP',
                                          passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
                                  
                            //def response = sh(script: "curl -H \"authorization: Bearer ${GITHUB_ACCESS_TOKEN}\" -H \"Accept: application/vnd.github.v3+json\" https://api.github.com/repos/apache/netbeans-jenkins-lib/pulls/${pullRequest.number}", returnStdout: true)
                            //def props = readJSON text: '{ "key": "value" }'
                            
                            def response = sh(script: "curl -H \"authorization: Bearer ${GITHUB_ACCESS_TOKEN}\" -H \"Accept: application/vnd.github.v3+json\" https://api.github.com/users/${CHANGE_AUTHOR}", returnStdout: true)
                            
                            message += response + NL
                        }
                                             
                                    
                        message += "Generation date: ${sh(returnStdout: true, script: "date '+%Y-%m-%d %H:%M:%S'").trim()}" + NL
                        
                        // https://github.com/jenkinsci/pipeline-github-plugin#commit
                        
                        for (commit in pullRequest.commits) {
                            message += "${commit.author}" + " " + "${commit.committer}" + NL
                        }
                        
                        // Check the existing comments of the PR, which are marked to come from this job
                        def jobComments = []
                        for (comment in pullRequest.comments) {
                            if (comment.body.contains(MARKER_COMMENT)) {
                                jobComments << comment
                            }
                        }
                        
                        if(jobComments.size() == 0) {
                            // If there is not existing bot comment, add a new comment with the summary
                            pullRequest.comment(message)
                        } else {
                            // If there is already one bot comment, the first bot comment is updated and the further bot comments are removed
                            pullRequest.editComment(jobComments[0].id, message)
                            for(int i = 1; i < jobComments.size(); i++) {
                                pullRequest.deleteComment(jobComments[i].id)
                            }
                        }
                        
                    }
                }
            }
        }
    }
}
