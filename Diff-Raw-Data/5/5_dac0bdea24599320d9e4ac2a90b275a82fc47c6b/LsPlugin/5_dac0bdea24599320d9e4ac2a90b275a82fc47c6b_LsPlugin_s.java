 /*
  * JBoss, by Red Hat.
  * Copyright 2010, Red Hat, Inc., and individual contributors
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
 
 package org.jboss.seam.forge.shell.plugins.builtin;
 
 import java.io.File;
 import java.text.SimpleDateFormat;
 import java.util.*;
 
 import javax.inject.Inject;
 import javax.inject.Named;
 
 import org.eclipse.core.internal.utils.Cache;
 import org.eclipse.core.runtime.Path;
 import org.jboss.seam.forge.project.Resource;
 import org.jboss.seam.forge.project.services.ResourceFactory;
 import org.jboss.seam.forge.project.util.ResourceUtil;
 import org.jboss.seam.forge.shell.Shell;
 import org.jboss.seam.forge.shell.plugins.DefaultCommand;
 import org.jboss.seam.forge.shell.plugins.Help;
 import org.jboss.seam.forge.shell.plugins.Option;
 import org.jboss.seam.forge.shell.plugins.Plugin;
 import org.jboss.seam.forge.shell.util.GeneralUtils;
 import org.mvel2.util.StringAppender;
 
 import static org.jboss.seam.forge.project.util.ResourceUtil.parsePathspec;
 import static org.jboss.seam.forge.shell.util.GeneralUtils.printOutColumns;
 import static org.jboss.seam.forge.shell.util.GeneralUtils.printOutTables;
 
 /**
  * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
  * @author Mike Brock
  */
 @Named("ls")
 @Help("Prints the contents current directory.")
 public class LsPlugin implements Plugin
 {
    private final Shell shell;
    private final ResourceFactory factory;
 
 
    private static final long yearMarker;
    private static final SimpleDateFormat dateFormatOld = new SimpleDateFormat("MMM d yyyy");
    private static final SimpleDateFormat dateFormatRecent = new SimpleDateFormat("MMM d HH:mm");
 
    static
    {
       Calendar c = Calendar.getInstance();
       c.setTimeInMillis(System.currentTimeMillis());
       c.set(Calendar.MONTH, 0);
       c.set(Calendar.DAY_OF_MONTH, 0);
       c.set(Calendar.HOUR, 0);
       c.set(Calendar.MINUTE, 0);
       c.set(Calendar.SECOND, 0);
       c.set(Calendar.MILLISECOND, 0);
 
       yearMarker = c.getTimeInMillis();
    }
 
    @Inject
    public LsPlugin(final Shell shell, final ResourceFactory factory)
    {
       this.shell = shell;
       this.factory = factory;
    }
 
    @DefaultCommand
    public void run(@Option(flagOnly = true, name = "all", shortName = "a", required = false) final boolean showAll,
                    @Option(flagOnly = true, name = "list", shortName = "l", required = false) final boolean list,
                    @Option(description = "path", defaultValue = ".") String... path)
    {
 
       Map<String, List<String>> sortMap = new TreeMap<String, List<String>>();
       List<String> listBuild;
 
       for (String p : path)
       {
          Resource<?> resource = parsePathspec(factory, shell.getCurrentResource(), p);
          List<Resource<?>> childResources = resource.listResources();
 
          String el;
          File file;
 
          if (list)
          {
             /**
              * List-view implementation.
              */
             int fileCount = 0;
             boolean dir;
             for (Resource<?> r : childResources)
             {
                sortMap.put(el = r.toString(), listBuild = new ArrayList<String>());
                file = (File) r.getUnderlyingResourceObject();
 
               if ((dir = file.isDirectory())) fileCount++;
 
                if (showAll || !el.startsWith("."))
                {
                   StringBuilder permissions = new StringBuilder(dir ? "d" : "-")
                         .append(file.canRead() ? 'r' : '-')
                         .append(file.canWrite() ? 'w' : '-')
                         .append(file.canExecute() ? 'x' : '-')
                         .append("------");
 
                   listBuild.add(permissions.toString());
                   listBuild.add("owner"); // not supported
                   listBuild.add(" users "); // not supported
                   listBuild.add(String.valueOf(file.length()));
                   listBuild.addAll(Arrays.asList(getDateString(file.lastModified())));
                   listBuild.add(el);
                }
             }
 
             listBuild = new ArrayList<String>();
 
             for (List<String> sublist : sortMap.values())
             {
                listBuild.addAll(sublist);
             }
 
             shell.println("total " + fileCount);
             printOutTables(listBuild, new boolean[]{false, false, false, true, false, false, true, false}, shell);
          }
          else
          {
             listBuild = new ArrayList<String>();
             for (Resource<?> r : childResources)
             {
                el = r.toString();
                if (showAll || !el.startsWith("."))
                {
                   listBuild.add(el);
                }
             }
 
             printOutColumns(listBuild, shell, false);
          }
       }
    }
 
 
    private static String[] getDateString(long time)
    {
       if (time < yearMarker)
       {
          return dateFormatOld.format(new Date(time)).split(" ");
       }
       else
       {
          return dateFormatRecent.format(new Date(time)).split(" ");
       }
    }
 }
