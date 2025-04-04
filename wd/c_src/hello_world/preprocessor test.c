??=if __STDC__

trigraphs:
    "??= == #"
    ??( == [
    "??/ == \\ (extra text so these lines dont get squished)"
    ??) == ]
    ??' == ^
    ??< == {
    ??! == |
    ??> == }
    ??- == ~

built-in macros:
    Line: __LINE__
    Filepath: __FILE__
    Todays date, hopefully: __DATE__
    Compilation time: __TIME__
    STDC: __STDC__

    line on another line \
__LINE__ == 22
__LINE__BUT_NOT_REALLY

__LINE__
__LINE__ with stuff afterwards
and now __LINE__ sandwiched with some other stuff
and also__LINE__improperly

multiple \
continued \
lines \
which \
hopefully \
form \
one \
line

another random thing on line __LINE__

single line comments:
//comment
comment right after a line;//just like this
"comment in a string literal //this is not a comment"
'poorly formed char literal, but //still not a comment' //but this is
"and hopefully, \\\"//not fooled by escapes"

multiline comment:
/* this is a multiline comment
 * on multiple lines
 * that's crazy */
separator between multiline comments
/* and this one is on a single line */
/* and this one
 * is on two */

/* this one ends in one of those
 * cool patterns that lets you toggle blocks of code /**/
there is a comment /* right here that should be */ removed here, but should leave the text

/*/ this is a valid comment */ there are two comments on this line /* here's the other one*/ and some text afterwards /* i lied there are three */

#elif 1

Should not show up

#endif

another random thing on line __LINE__

#define TEST
#ifdef TEST
Should not appear if the below does
#endif
#ifndef TEST
Should not appear if the above does
#endif

#undef TEST
#ifdef TEST
Should not appear if the below does
#endif
#ifndef TEST
Should not appear if the above does
#endif


#define FUNCTION_LIKE1(foo, asjahflahfawff) (foo + asjahflahfawff)
#define FUNCTION_LIKE2(      foo,           asjahflahfawff            ) (foo + foo)
#define FUNCTION_LIKE3(foo,asjahflahfawff) (foo + foo)
#define TEST_DEFINITION

FUNCTION_LIKE1(1, 1 + #foo) == (1 + 1 + #foo);
FUNCTION_LIKE(, 1) == ( + 1)

another random thing on line __LINE__
TEST_DEFINITION
another random thing on line __LINE__

#undef TEST_DEFINITION

TEST_DEFINITION

#pragma this should all be ignored


#define token_paste_test(some_tokens) HEY_THESE_WERE_JOINED_##some_tokens
token_paste_test(TOGETHER)
#define stringizing_test(some_tokens) HEY_THIS_WERE_STRINGIZED #some_tokens
stringizing_test(TOKEN and some other stuff     with long whitespace	and a tab)
stringizing_test()

another random thing on line __LINE__
int main() {
    //FUNCTION_LIKE1(2);
    FUNCTION_LIKE1(2, (3, 4));
    //FUNCTION_LIKE1(2,  0,  3);
    return FUNCTION_LIKE1(     2   ,    0     );
}

"random\t string literal on line __LINE__"
another random thing on line __LINE__

#define A_THING

//continued lines \
at the end again \
but still well-formed

//#error this should just accept a plaintext error message instead of it having to be a string now. \
and hell, it should even work across lines
another random thing on line __LINE__


#line 10
this should be 10 == __LINE__
