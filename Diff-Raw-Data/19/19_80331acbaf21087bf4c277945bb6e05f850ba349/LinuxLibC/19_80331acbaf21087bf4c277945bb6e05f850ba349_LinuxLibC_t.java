 package org.jruby.ext.posix;
 
 public interface LinuxLibC extends LibC {
    public int __fxstat(int version, int fd, FileStat stat);
     public int __lxstat(int version, String path, FileStat stat);
     public int __xstat(int version, String path, FileStat stat);
 }
