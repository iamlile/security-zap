package com.thoughtworks.tools.security.zap.task.core

import com.thoughtworks.tools.security.zap.utils.ZapClient
import groovyx.net.http.RESTClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import static org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS

class ZapStartTask extends DefaultTask {
    @TaskAction
    def zapStart() {
        println "Starting ZAP [apikey: ${project.zap.server.apiKey}]"
        launchZapServer()
    }

    def launchZapServer() throws IOException {
        ProcessBuilder builder = null

        if (IS_OS_WINDOWS) {
            builder = new ProcessBuilder("zap.bat",
                    "-config", "api.key=${project.zap.server.apiKey}",
                    "-daemon")
            builder.directory(new File("${project.zap.server.home}"))
        } else {
             builder = new ProcessBuilder("${project.zap.server.home}zap.${determineFileExtension()}",
                    "-config", "api.key=${project.zap.server.apiKey}",
                    "-daemon")
        }

        builder.start();
    }

    def verifyZapStarted() {
        def client = new RESTClient("http://zap")
        client.setProxy(project.zap.server.host, project.zap.server.port, "http")
        def counter = 0;
        def status = 0;

        while (true) {
            println "waiting ZAP"

            try {
                status = client.get(path: "/").status
            } catch (Exception e) {}

            if (status == 200) {
                println "ZAP started"
                break
            } else {
                if (counter >= 10) {
                    println "fail to start ZAP due to timeout"
                    break
                }

                sleep(5 * 1000)
                counter += 1
            }
        }
    }

    def applyExclusion() {
        def zapClient = new ZapClient(project.zap.server.host, project.zap.server.port, project.zap.server.apiKey)

        zapClient.clearExcludedFromProxy()
        zapClient.excludeFromProxy(project.zap.target.exclude)
        zapClient.viewExcludedUrls()
    }

    def determineFileExtension() {
        IS_OS_WINDOWS ? 'bat' : 'sh'
    }
}
