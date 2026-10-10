package com.fooddecider.app.repository;

import com.fooddecider.app.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, String> {
    List<RoomMember> findByRoomCode(String roomCode);
}
