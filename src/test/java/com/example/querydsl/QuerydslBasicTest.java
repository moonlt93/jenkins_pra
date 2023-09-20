package com.example.querydsl;

import com.example.querydsl.dto.MemberDto;
import com.example.querydsl.dto.QMemberDto;
import com.example.querydsl.dto.UserDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.QTeam;
import com.example.querydsl.entity.Team;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializer;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory ;


    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);

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

    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

       Member fetchFirst=
               queryFactory.selectFrom(member)
                .fetchOne();

//
//        List<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
        // 제대로 JPQL 에서 카운트 하는 쿼리가 안 먹힌다구 함.


        List<Member> content = queryFactory
                .selectFrom(member)
                .where(member.username.like("member"))
                .fetch();

        // 이후 생성자에 content, pageable, content.size();등으로 넘기는게 나아보임

    }

/*
* 회원 정렬 순서
* 1.회원 나이 내림차순
* 2.회원 이름 올림차순
* 단 2에서 회원 이름이 없으면 마지막에 출력(null last)
* */

    @Test
    public void sort() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);

        Member memberNULl = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNULl.getUsername()).isNull();


    }

    @Test
    public void paging1(){
    List<Member> list = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();


    }

    @Test
    public void paging2(){

        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getResults().size()).isEqualTo(2);


    }

    @Test
    public void aggregate(){
List<Tuple> result =
        queryFactory.select(
        member.count(),
        member.age.sum(),
        member.age.avg(),
        member.age.max(),
        member.age.min()

).from(member).fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);



    }


    /*
    팀의 이름과 각 팀의 평균 연령을 구하여라
    * */
    @Test
    public void groupby(){
    //given
    List<Tuple> result = queryFactory
            .select(team.name,member.age.avg())
            .from(member)
            .join(member.team,team)
            .groupBy(team.name)
            .fetch();

    Tuple teamA= result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);



    //when

    //then

    }



    @Test
    public void dtoSearch(){
    //given

    List<MemberDto> list =    queryFactory.select(Projections.bean(MemberDto.class,
                member.username,
                member.age))
                .from(member)
                .fetch();

        for (MemberDto mem: list
             ) {
            System.out.println("[memberDto]"+ mem);
        }


    //when
    //then

    }


    /* field 에 꼳힘 */
    @Test
    public void dtofield(){
        //given

        List<MemberDto> list =
                queryFactory.select(
                        Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto mem: list
        ) {
            System.out.println("[memberDto]"+ mem);
        }


        //when
        //then

    }

    /* 생성자 방법 */
    @Test
    public void dtoConstructor(){
        //given

        List<MemberDto> list =
                queryFactory.select(
                                Projections.constructor(MemberDto.class,
                                        member.username,
                                        member.age))
                        .from(member)
                        .fetch();

        for (MemberDto mem: list
        ) {
            System.out.println("[memberDto]"+ mem);
        }


        //when
        //then

    }



    /* 생성자 방법 */
    @Test
    public void findUserDto(){
        //given
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result =
                queryFactory.select(
                                Projections.fields(UserDto.class,
                                        member.username.as("name"),
                                        ExpressionUtils.as(JPAExpressions
                                                .select(memberSub.age.max())
                                                .from(memberSub),"age")
                                        ))
                        .from(member)
                        .fetch();

        for (UserDto mem: result
        ) {
            System.out.println("[memberDto]"+ mem);
        }


        //when
        //then

    }


    //@QueryProjection Q파일 생성해야됨.
    // MemberDto가 쿼리 dsl 의존성이 생김

    @Test
    public void findByQueryProjection(){
    //given
        List<MemberDto> result =
        queryFactory.select(new QMemberDto(member.username,member.age))
            .from(member)
            .fetch();


        for (MemberDto dto:result
             ) {
            System.out.println("dtos"+dto);
        }
    //when
    //then

    }



    @Test
    public void booleanBuilders(){
    //given
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result= searchmember1(usernameParam,ageParam);
    assertThat(result.size()).isEqualTo(1);

    //when
    //then

    }

    private List<Member> searchmember1(String usernameParam, Integer ageParam) {
        BooleanBuilder builder = new BooleanBuilder();

        if(usernameParam != null){
            builder.and(member.username.eq(usernameParam));
        }
        if(ageParam != null){
            builder.and(member.age.eq(ageParam));
        }



        return queryFactory
             .selectFrom(member)
             .where(builder)
             .fetch();


    }


}
