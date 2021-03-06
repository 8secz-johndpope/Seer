 /**
  * Copyright (C) 2011
  *   Michael Mosmann <michael@mosmann.de>
  *   Martin Jöhren <m.joehren@googlemail.com>
  *
  * with contributions from
  * 	konstantin-ba@github,Archimedes Trajano (trajano@github)
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
 package de.flapdoodle.embed.nodejs;
 
 import java.util.regex.Pattern;
 
 import de.flapdoodle.embed.process.config.store.ArtifactStoreInUserHome;
 import de.flapdoodle.embed.process.config.store.IArtifactStoragePathNaming;
 import de.flapdoodle.embed.process.config.store.IDownloadConfig;
 import de.flapdoodle.embed.process.config.store.IPackageResolver;
 import de.flapdoodle.embed.process.distribution.ArchiveType;
 import de.flapdoodle.embed.process.distribution.Distribution;
 import de.flapdoodle.embed.process.extract.ITempNaming;
 import de.flapdoodle.embed.process.extract.UUIDTempNaming;
 import de.flapdoodle.embed.process.io.progress.IProgressListener;
 import de.flapdoodle.embed.process.io.progress.StandardConsoleProgressListener;
 
 public class NodejsDownloadConfig implements IDownloadConfig {
 
 	private ITempNaming fileNaming = new UUIDTempNaming();
 
 	private String downloadPath = "http://nodejs.org/dist/";
 
 	private IProgressListener progressListener = new StandardConsoleProgressListener();
	private IArtifactStoragePathNaming artifactStorePathNaming = new ArtifactStoreInUserHome(".nodejs");
 
 	private String downloadPrefix = "nodejs-download";
 
 	private String userAgent = "Mozilla/5.0 (compatible; "
 			+ "Embedded node.js; +https://github.com/flapdoodle-oss/embedmongo.flapdoodle.de)"; // change to embednodejs
 
 	private NodejsPaths _packageResolver = new NodejsPaths();
 
 	@Override
 	public String getDownloadPath() {
 		return downloadPath;
 	}
 
 	@Override
 	public IProgressListener getProgressListener() {
 		return progressListener;
 	}
 
 	@Override
 	public IArtifactStoragePathNaming getArtifactStorePathNaming() {
		return artifactStorePathNaming;
 	}
 
	public void setArtifactStorePathNaming(IArtifactStoragePathNaming artifactStorePath) {
		this.artifactStorePathNaming = artifactStorePath;
 	}
 
 	@Override
 	public ITempNaming getFileNaming() {
 		return fileNaming;
 	}
 
 	@Override
 	public String getDownloadPrefix() {
 		return downloadPrefix;
 	}
 
 	@Override
 	public String getUserAgent() {
 		return userAgent;
 	}
 
 	@Override
 	public IPackageResolver getPackageResolver() {
 		return _packageResolver;
 	}
 	
 	public void setPackageResolver(NodejsPaths packageResolver) {
 		_packageResolver = packageResolver;
 	}
 
 	public void setFileNaming(ITempNaming fileNaming) {
 		this.fileNaming = fileNaming;
 	}
 
 	public void setDownloadPath(String downloadPath) {
 		this.downloadPath = downloadPath;
 	}
 
 	public void setProgressListener(IProgressListener progressListener) {
 		this.progressListener = progressListener;
 	}
 
 	public void setDownloadPrefix(String downloadPrefix) {
 		this.downloadPrefix = downloadPrefix;
 	}
 
 	public void setUserAgent(String userAgent) {
 		this.userAgent = userAgent;
 	}
 
 }
