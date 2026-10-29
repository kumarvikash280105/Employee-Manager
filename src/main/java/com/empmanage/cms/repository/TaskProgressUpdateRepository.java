package com.empmanage.cms.repository;

import com.empmanage.cms.entity.TaskProgressUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskProgressUpdateRepository extends JpaRepository<TaskProgressUpdate, Long> {

    List<TaskProgressUpdate> findByTaskIdOrderByTimestampDesc(Long taskId);

    List<TaskProgressUpdate> findByTask_IdOrderByTimestampDesc(Long taskId);
}
