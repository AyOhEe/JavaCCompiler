#include <angled include>
#include "quote include"

defined(), sizeof()
<<=, >>=, ...

++, --, <<, >>
<=, >=, ==, !=
&&, ||
*=, /=, %=, +=, -=, &=, ^=, |=
->, ##

[, ], (, ), {, }, .
&, *, +, -, ~, !, /, %
<, >, ^, |,
?, :, =, ,, #, ;

(nested -               ( expression + with / some.stuff))

___valid_Identifier1234567890___
not-an-identifier

should insert \
blank line here
so this is still line25

#if defined(FLAGS_WORKED)

#endif

"string_literal with spaces     and long gaps" and then 'a character literal that\'s way too long'
"testing escapes \' \n \b \r \\\\\t \\t \" the string should still continue \\" but should stop there
