package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.example.querydsl.entity.QMember.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    @BeforeEach
    public void before(){
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
    }

    @Test
    public void startJPQL(){
        //member1을 찾아라.
        String qlString = "select m from Member m " +
                "where m.username= :username";

    Member findMember=  em.createQuery("select m from Member m where m.username= :username", Member.class)
                .setParameter("username","member1")
                .getSingleResult();


        assertThat(findMember.getUsername()).isEqualTo("member1");

    }


    @Test
    public void startQuerydsl(){

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");


    }


    @Test
    public void search(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        Member m = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10))).fetchOne();

        assertThat(m.getUsername()).isEqualTo("member1");

    }

    @Test
    public void searchAndParam(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        Member m = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        ,member.age.eq(10) // and
                ).fetchOne();

        assertThat(m.getUsername()).isEqualTo("member1");

    }


}
