#if 1

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

single line comment:
//comment

multiline comment:
/* this is a multiline comment
 * on multiple lines
 * that's crazy */
separator between multiline comments
/* and this one is on a single line */

#endif

int main() {
    return __LINE__;
}