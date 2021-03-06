 package no.java.incogito.domain;
 
 import fj.F;
 import fj.pre.Equal;
 import fj.data.List;
 import fj.data.Option;
 import no.java.incogito.Enums;
 import org.joda.time.Interval;
 
 /**
  * @author <a href="mailto:trygve.laugstol@arktekk.no">Trygve Laugst&oslash;l</a>
  * @version $Id$
  */
 public class Session {
     public enum Format {
         Presentation,
         Quickie,
         BoF;
 
         public static Equal<Format> equal = Enums.<Format>equal();
         public static F<String, Option<Format>> valueOf_ = Enums.<Format>valueOf().f(Format.class);
         public static F<Format, String> toString = Enums.toString_();
     }
 
     public final SessionId id;
     public final Format format;
     public final String title;
     public final Option<WikiString> body;
     public final Option<Level> level;
     public final Option<Interval> timeslot;
     public final Option<String> room;
     public final List<Label> labels;
     public final List<Speaker> speakers;
     public final List<Comment> comments;
 
     public Session(SessionId id, Format format, String title, Option<WikiString> body, Option<Level> level,
                    Option<Interval> timeslot, Option<String> room, List<Label> labels, List<Speaker> speakers,
                    List<Comment> comments) {
         this.id = id;
         this.format = format;
         this.title = title;
         this.body = body;
         this.level = level;
         this.timeslot = timeslot;
         this.room = room;
         this.labels = labels;
         this.speakers = speakers;
         this.comments = comments;
     }
 
    public Session title(String title) {
        return new Session(id, format, title, body, level, timeslot, room, labels, speakers, comments);
    }

     public Session timeslot(Option<Interval> timeslot) {
         return new Session(id, format, title, body, level, timeslot, room, labels, speakers, comments);
     }
 
     public Session room(Option<String> room) {
         return new Session(id, format, title, body, level, timeslot, room, labels, speakers, comments);
     }
 }
