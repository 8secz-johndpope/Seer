 /*
  * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
  * written by Rasto Levrinc.
  *
  * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
  *
  * DRBD Management Console is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License as published
  * by the Free Software Foundation; either version 2, or (at your option)
  * any later version.
  *
  * DRBD Management Console is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with drbd; see the file COPYING.  If not, write to
  * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
  */
 
 package drbd.configs;
 
 import java.util.Arrays;
 
 /**
  * Here are commands for rhel version 4.
  */
 public class DistResource_redhatenterpriseas_4 extends
             java.util.ListResourceBundle {
 
     /** Get contents. */
     protected final Object[][] getContents() {
         return Arrays.copyOf(contents, contents.length);
     }
 
     /** Contents. */
     private static Object[][] contents = {
         /* distribution name that is used in the download url */
         {"distributiondir", "rhel4"},
 
         /* support */
         {"Support", "redhatenterpriseas-4"},
 
         {"DrbdAvailVersions",
          "/usr/bin/wget -q http://www.linbit.com/@SUPPORTDIR@/ -O - |perl -ple '($_) = /href=\"@DRBDDIR@-(\\d.*?)\\/\"/ or goto LINE'"
         },
 
         {"DrbdAvailDistributions",
          "/usr/bin/wget -q http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/ -O - |perl -ple '($_) = m!href=\"([^\"/]+)/\"! or goto LINE'"
         },
 
         {"DrbdAvailKernels",
          "/usr/bin/wget -q http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/ -O - |perl -ple '($_) = m!href=\"([^\"/]+)/\"! or goto LINE'"
         },
 
         {"DrbdAvailArchs",
          "/usr/bin/wget -q http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/ -O - |perl -ple '($_) = m!href=\"drbd8?-(?:plus8?-)?(?:km|module)-.+?(i386|x86_64|amd64|i486|i686|k7)\\.(?:rpm|deb)\"! or goto LINE'"
         },
 
         {"DrbdAvailBuilds",
          "/usr/bin/wget -q http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/ -O - |perl -ple '($_) = m!href=\"drbd8?-(?:plus8?-)?(?:km|module)-(.*?)[-_]@DRBDVERSION@.+?[._]@ARCH@\\..+?\"! or goto LINE'"
         },
 
         {"DrbdAvailVersionsForDist",
          "/usr/bin/wget -q http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/ -O - |perl -ple '($_) = m!href=\"drbd8?-(?:plus8?-)?(?:utils_)?(\\d.*?)-\\d+[._]@ARCH@\\..+?\"! or goto LINE'"
         },
 
         {"DrbdAvailFiles",
          "/usr/bin/wget -q http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/ -O - |perl -ple '($_) = m!href=\"(drbd8?-(?:plus8?-)?(?:utils)?(?:(?:km|module|utils)[_-]@BUILD@)?[-_]?@DRBDVERSION@.*?[._]@ARCH@\\.(?:rpm|deb))\"! or goto LINE'"
         },
        // TODO: --no-check-certificate does not work on older rhel4
        {"DrbdInst.wget",    "/usr/bin/wget --no-check-certificate --progress=dot --http-user='@USER@' --http-passwd='@PASSWORD@' --directory-prefix=/tmp/drbdinst/ "
          + "http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/@DRBDPACKAGE@ "
          + "http://www.linbit.com/@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/@DRBDMODULEPACKAGE@"},
 
         {"HbInst.install.text.1", "http://download.opensuse.org: wget & rpm -U" },
         {"HbInst.install.1", "rm -rf /tmp/drbd-mc-hbinst/; "
                            + "mkdir /tmp/drbd-mc-hbinst/ && "
                            + "wget -nd -r -np --progress=dot -P /tmp/drbd-mc-hbinst/ http://download.opensuse.org/repositories/server:/ha-clustering/RHEL_4/@ARCH@/ && "
                            + "rm /tmp/drbd-mc-hbinst/pacemaker-mgmt-*.rpm && "
                            + "rm /tmp/drbd-mc-hbinst/heartbeat-ldirectord-*.rpm && "
                            + "up2date libtool-libs perl-TimeDate && "
                            + "rpm -Uvh /tmp/drbd-mc-hbinst/*.rpm && "
                            + "rm -rf /tmp/drbd-mc-hbinst/"},
     };
 }
