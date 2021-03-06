 package com.thoughtworks.twu.persistence;
 
 import com.thoughtworks.twu.domain.Presentation;
 import com.thoughtworks.twu.domain.Talk;
 import org.apache.ibatis.annotations.*;
 import org.joda.time.DateTime;
 
 import java.util.List;
 
 public interface TalkMapper {
 
    @Insert("INSERT INTO talk (presentation_id,venue, time_of_talk, time_of_creation) VALUES(#{presentation.id}, #{venue}, #{dateTime},#{timeOfCreation})")
     int insert(Talk talk);
 
 
    @Select("SELECT talk_id, presentation_id, venue, time_of_talk, time_of_creation FROM talk WHERE talk_id =  #{talkId}")
     @Results(value = {
             @Result(property="talkId", column="talk_id"),
             @Result(property="venue", column="venue"),
             @Result(property="dateTime", column="time_of_talk"),
            @Result(property="timeOfCreation", column = "time_of_creation"),
             @Result(property="presentation", column="presentation_id", javaType=Presentation.class, one=@One(select="selectPresentation"))
     })
     Talk getTalk(int talkId);
 
     @Select("SELECT id,title,description,owner FROM presentation where id = #{presentationId}")
     Presentation selectPresentation(int presentationId);
 
     @Select("SELECT talk_id FROM talk WHERE talk_id=IDENTITY()")
     int getLastId();
 
     @Select("SELECT talk_id,title,description,owner,venue,time_of_talk FROM presentation JOIN talk ON presentation.id=talk.presentation_id WHERE presentation.owner=#{owner} ORDER BY time_stamp DESC")
     @Results(value = {
             @Result(property="talkId", column="talk_id"),
             @Result(property="presentation.title", column="title"),
             @Result(property = "presentation.description",column="description"),
             @Result(property = "presentation.owner",column="owner"),
             @Result(property="venue", column="venue"),
             @Result(property="dateTime", column="time_of_talk")
     })
     List<Talk> getTalksByUsername(String owner);
 
     @Select("SELECT talk_id,title,description,owner,venue,time_of_talk " +
             "FROM presentation " +
             "JOIN talk ON presentation.id=talk.presentation_id " +
             "WHERE talk.time_of_talk > #{since} and talk.time_of_talk < #{to}" +
             "ORDER BY talk.time_of_talk DESC")
     @Results(value = {
             @Result(property="talkId", column="talk_id"),
             @Result(property="presentation.title", column="title"),
             @Result(property = "presentation.description",column="description"),
             @Result(property = "presentation.owner",column="owner"),
             @Result(property="venue", column="venue"),
             @Result(property="dateTime", column="time_of_talk")
     })
     List<Talk> getTalks(@Param("since") DateTime since, @Param("to") DateTime to);
 
 
     @Delete("DELETE FROM talk WHERE talk_id=#{talkId}")
     int deleteById(int talkId);
 
 }
