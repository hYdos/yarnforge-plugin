/*
 * Copyright 2020 Ramid Khan
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

package me.ramidzkh.yarnforge;

import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;
import net.minecraftforge.gradle.common.util.MappingFile;
import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import net.minecraftforge.gradle.common.util.McpNames;
import net.minecraftforge.gradle.common.util.MinecraftExtension;
import net.minecraftforge.gradle.mcp.MCPRepo;
import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.options.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;
import java.util.zip.ZipFile;

public class BaseRemappingTask extends DefaultTask {

    private String mappings;
    protected Supplier<File> srg;

    @Option(description = "Mappings", option = "mappings")
    public void setMappings(String mappings) {
        this.mappings = mappings;
    }

    public void setSrgProvider(Supplier<File> srg) {
        this.srg = srg;
    }

    protected MappingSet createMcpToYarn() throws IOException {
        Project project = getProject();
        String names;

        MinecraftExtension extension = project.getExtensions().findByType(MinecraftExtension.class);

        if (extension != null) {
            names = extension.getMappings();
        } else {
            names = project.project(":clean").getExtensions().findByType(MinecraftExtension.class).getMappings();
        }

        MappingSet obfToMcp = MappingBridge.mergeMcpNames(MappingBridge.loadMappingFile(MappingSet.create(), MappingFile.load(srg.get())), McpNames.load(findNames(names)));
        MappingSet obfToYarn = MappingBridge.loadTiny(MappingSet.create(), loadTree(project, mappings), "official", "named");
        return obfToMcp.reverse().merge(obfToYarn);
    }

    private File findNames(String mapping) {
        int idx = mapping.lastIndexOf('_');
        String channel = mapping.substring(0, idx);
        String version = mapping.substring(idx + 1);
        String desc = MCPRepo.getMappingDep(channel, version);

        return MavenArtifactDownloader.generate(getProject(), desc, false);
    }

    public static TinyTree loadTree(Project project, String mappings) throws IOException {
        try (ZipFile archive = new ZipFile(project.getConfigurations().detachedConfiguration(project.getDependencies().create(mappings)).getSingleFile());
             BufferedReader reader = new BufferedReader(new InputStreamReader(archive.getInputStream(archive.getEntry("mappings/mappings.tiny"))))) {
            return TinyMappingFactory.loadWithDetection(reader);
        }
    }
}
