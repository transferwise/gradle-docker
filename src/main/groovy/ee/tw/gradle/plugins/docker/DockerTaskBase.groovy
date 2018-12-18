/**
 * Copyright 2014 Transmode AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ee.tw.gradle.plugins.docker


import org.gradle.api.DefaultTask
import com.google.common.annotations.VisibleForTesting
import ee.tw.gradle.plugins.docker.client.NativeDockerClient

abstract class DockerTaskBase extends DefaultTask {

    @VisibleForTesting
    static final String LATEST_VERSION = 'latest'
    
    // Name of the application being wrapped into a docker image (default: project.name)
    String applicationName
    // What to tag the created docker image with (default: group/applicationName)
    String tag
    // Which versions to use along with the tag (default: latest)
    Set<String> tagVersions
    // Which version to use along with the tag (default: latest).
    String tagVersion
    // Hostname, port of the docker image registry unless Docker index is used
    String registry

    // Should we use Docker's remote API instead of the docker executable
    Boolean useApi
    
    // Full path to the docker executable
    String dockerBinary
    
    // URL of the remote Docker host (default: localhost)
    String hostUrl
    
    // Docker remote API credentials
    String apiUsername
    String apiPassword
    String apiEmail
    
    DockerTaskBase() {
        applicationName = project.name
    }

    public void setTagVersions(Set<String> versions) {
        tagVersions = versions
    }

    public void setTagVersion(String version) {
        tagVersion = version
    }

    void setTagVersionToLatest() {
        tagVersion = LATEST_VERSION;
    }

    protected Set<String> getImageTags() {
        String tag
        tag = this.tag ?: getDefaultImageTag()
        return appendImageTagVersions(tag)
    }

    private String getDefaultImageTag() {
        String tag
        if (registry) {
            def group = project.group ? "${project.group}/" : ''
            tag = "${-> registry}/${group}${-> applicationName}"
        } else if (project.group) {
            tag = "${-> project.group}/${-> applicationName}"
        } else {
            tag = "${-> applicationName}"
        }
        return tag
    }

    private Set<String> appendImageTagVersions(String name) {
        def versions
        if(tagVersions) {
            versions = tagVersions
        } else if(tagVersion) {
            versions = [ tagVersion ]
        } else if(project.version != 'unspecified') {
            versions = [ project.version ]
        } else {
            versions = [ LATEST_VERSION ]
        }
        return versions.collect { version -> "${name}:${version}".toString() }
    }

    protected ee.tw.gradle.plugins.docker.client.DockerClient getClient() {
        ee.tw.gradle.plugins.docker.client.DockerClient client
        if(getUseApi()) {
            logger.info("Using the Docker remote API.")
            client = ee.tw.gradle.plugins.docker.client.JavaDockerClient.create(
                    getHostUrl(),
                    getApiUsername(),
                    getApiPassword(),
                    getApiEmail())
        } else {
            logger.info("Using the native docker binary.")
            client = new NativeDockerClient(getDockerBinary())
        }
        return client
    }

}
