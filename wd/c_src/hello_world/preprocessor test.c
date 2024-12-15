#if __STDC__

trigraphs:
    ??= == #
    ??( == [
    ??/ == \ (extra text so these lines dont get squished)
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

#if 0 //REINTRODUCE AFTER CONSTEXPR AND DEFINITIONS WORK
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
#endif


#define FUNCTION_LIKE1(foo, asjahflahfawff) (foo + foo)
#define FUNCTION_LIKE2(      foo,           asjahflahfawff            ) (foo + foo)
#define FUNCTION_LIKE3(foo,asjahflahfawff) (foo + foo)
#define TEST_DEFINITION

TEST_DEFINITION

#undef TEST_DEFINITION

TEST_DEFINITION


int main() {
    //FUNCTION_LIKE1(2);
    FUNCTION_LIKE1(2, 0);
    //FUNCTION_LIKE1(2,  0,  3);
    return FUNCTION_LIKE1(     2   ,    0     );
}

"random string literal on line __LINE__"

#define A_THING

//continued lines \
at the end again \
but still well-formed
