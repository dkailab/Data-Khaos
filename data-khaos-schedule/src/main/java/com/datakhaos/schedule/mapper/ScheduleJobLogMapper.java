package com.datakhaos.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.schedule.entity.ScheduleJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface ScheduleJobLogMapper extends BaseMapper<ScheduleJobLog> {

    /** 任务最近一次开始时间 */
    @Select("SELECT MAX(start_time) FROM schedule_job_log WHERE job_id = #{jobId}")
    LocalDateTime selectLastStartTime(@Param("jobId") String jobId);
}
