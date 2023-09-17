package com.example.querydsl.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class MemberTest {

    @PersistenceContext
    EntityManager em;

    @Test
    public void testEntity(){

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",20,teamA);

        Member member3 = new Member("member3",30,teamB);
        Member member4 = new Member("member4",40,teamB);


        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //초기화
        em.flush();
        //영속성 컨텍스트 오브젝트 쿼리를 디비에 날림
        em.clear();
        //영속성 컨텍스트를 초기화 해서 캐시 날라감 쿼리 깔끔해짐

    List<Member> members=  em.createQuery("select m from Member m ",Member.class).getResultList();

        for (Member member: members
             ) {
            System.out.println("[member]" +member);
            System.out.println("[member.team]"+member.getTeam());
        }

    }

}