import ply.lex as lex
import ply.yacc as yacc

__author__ = 'bvan'

tokens = ('IDENT','NUMBER','DATE','STRING','BOOLEAN', 'NULL',
           'AND','OR','EQ','NOT_EQ','LT','GT','LTEQ','GTEQ',
           'RANGE','IN','NOT_IN','MATCHES',
           'LPAREN','RPAREN','OP','PRE','COMMA')

'''
Internally, lex.py uses the re module to do its patten matching.
When building the master regular expression, rules are added in the following order:

1. All tokens defined by functions are added in the same order as they appear in the lexer file.
2. Tokens defined by strings are added next by sorting them in order of decreasing regular expression length
    (longer expressions are added first).

'''

class Node:
    def __init__(self, val=None, type=-1, left=None, right=None):
        self.val = val
        self.type=type
        self.left=left
        self.right=right

    def __str__(self):
        s = " " + str(self.val) + " "
        if self.left is not None:
            s = str(self.left) + s
        if self.right is not None:
            s += str(self.right)
        return s if self.left is None and self.right is None else "( " + s + " )"

class BoolOp:
    EOF  = 1
    ERROR = 2
    AND = 3
    OR = 4
    EQ = 5
    NOT_EQ = 6
    GT = 7
    GTEQ = 8
    LT = 9
    LTEQ = 10
    IN = 11
    NOT_IN = 12
    LPAREN = 13
    RPAREN = 14
    COMMA = 15
    ELLIP = 16
    MATCHES = 17
    NULL = 18

def t_BOOLEAN(t):
    r'true|false'
    return t

def t_NULL(t):
    r'null|none'
    return t

def t_NOT_EQ(t):
    r'not\ eq(uals?)?|!eq(uals?)?|neq?|is\ not|!='
    return t

def t_AND(t):
    r'and|&&'
    return t

def t_OR(t):
    r'or|\|\|'
    return t

def t_LTEQ(t):
    r'lteq|le|<='
    return t

def t_LT(t):
    r'lt|<'
    return t

def t_GTEQ(t):
    r'gteq|ge|>='
    return t

def t_GT(t):
    r'gt|>'
    return t

def t_IN(t):
    r'in'
    return t

def t_NOT_IN(t):
    r'not in'
    return t

def t_MATCHES(t):
    r'=~|matches'
    return t

def t_EQ(t):
    r'is|eq(uals?)?|==|='
    return t

def t_LPAREN(t):
    r'\('
    return t

def t_RPAREN(t):
    r'\)'
    return t

def t_COMMA(t):
    r','
    return t

def t_RANGE(t):
    r':|->|to'
    return t

def t_NUMBER(t):
    r'[+-]?\d+(?:\.\d+)?(?:[eE][+-]\d+)?'
    try:
        t.value = int(t.value)
    except ValueError:
        try:
            t.value = float(t.value)
        except ValueError:
            print("Number value too large %d", t.value)
            t.value = 0
    return t


def t_STRING(t):
    r'''([durt]|ts)?'[^\r\n\'\\]*'|([durt]|ts)?"[^\r\n\"\\]*"'''
    if(t.value[0] not in ('"',"'")):
        print "special str"
    t.value = t.value[1:-1]
    return t

t_IDENT           = r'[a-zA-Z_\$][a-zA-Z0-9_\.\-\:\$]*'

# Ignored characters
t_ignore          = " \t"

def t_newline(t):
    r'\n+'
    t.lexer.lineno += t.value.count("\n")

def t_error(t):
    print("Illegal character '%s'" % t.value[0])
    t.lexer.skip(1)

# Build the lexer

lex.lex()
squer = "hello eq 2 and note gt 4"
squer2 = "_:-$hello eq 2 and note gt 4 and (fake is not null or s in 6:8)"
squer3 = "hello.hi eq 24 and note gt -4 and (fake is '252' or s not equals 6)"
squer4 = "hello.hi eq 2 and note gt 4 and (fake not eq 2.52 || s neq 6)"


lex.input(squer2)
sq2 = [str(repr(tok.type)) + ":" + str(repr(tok.value)) for tok in iter(lex.token, None)]
lex.input(squer3)
sq3 = [str(repr(tok.type)) + ":" + str(repr(tok.value)) for tok in iter(lex.token, None)]
lex.input(squer4)
sq4 = [str(repr(tok.type)) + ":" + str(repr(tok.value)) for tok in iter(lex.token, None)]


for i in range(0,len(sq2)):
    print sq2[i]

for i in range(0,len(sq3)):
    print sq3[i] + " -- " + sq4[i]

def p_ast(p):
    'ast : expr'
    p[0] = p[1]

def p_expr(p):
    '''expr : expr_or
            | expr AND expr_or'''
    if len(p) == 2:
        p[0] = p[1]
    else:
        p[0] = Node(p[2], p[2], p[1], p[3])

def p_expr_or(p):
    '''expr_or : simple_expr
               | expr_or OR simple_expr'''
    if len(p) == 2:
        p[0] = p[1]
    else:
        p[0] = Node(p[2], p[2], p[1], p[3])

def p_simple_expr(p):
    '''simple_expr : rel_expr
                   | LPAREN expr RPAREN'''
    if len(p) == 2:
        p[0] = p[1]
    else:
        setattr(p[2],'enclosed',True)
        p[0] = p[2]

def p_rel_expr(p):
    """
    rel_expr : rel_eq value
             | rel_eq BOOLEAN
             | rel_eq NULL
             | rel_matches STRING
             | rel_comp value
             | rel_in safe_list
             | rel_in LPAREN safe_list RPAREN
             | rel_in safe_range
             | rel_in LPAREN safe_range RPAREN"""
    if len(p) == 3:
        p[1].right = p[2]
    else:
        setattr(p[3],'enclosed',True)
        p[1].right = p[3]
    p[0] = p[1]


def p_rel_matches(p):
    'rel_matches : ident MATCHES'
    p[0] = Node(p[2], p[2], p[1])

def p_rel_eq(p):
    '''rel_eq : ident EQ
              | ident NOT_EQ'''
    p[0] = Node(p[2], p[2], p[1])

def p_rel_comp(p):
    '''rel_comp :  ident LT
        |          ident LTEQ
        |          ident GT
        |          ident GTEQ
    '''
    p[0] = Node(p[2], p[2], p[1])

def p_rel_in(p):
    '''rel_in : ident IN
              | ident NOT_IN'''
    p[0] = Node(p[2], p[2], p[1])

def p_safe_list(p):
    '''safe_list : num_list
                 | str_list'''
    p[0] = p[1]

def p_str_list(p):
    '''str_list : STRING
                | str_list COMMA STRING'''
    safelist(p)

def p_num_list(p):
    '''num_list : NUMBER
                | num_list COMMA NUMBER'''
    safelist(p)

def safelist(p):
    if len(p) == 2:
        p[0] = Node([p[1]], "LIST" + str(type(p[1])))
    else:
        p[1].val.append(p[3])
        p[0] = p[1]

def p_safe_range(p):
    '''safe_range : NUMBER RANGE NUMBER
                  | STRING RANGE STRING'''
    p[0] = Node((p[1], p[3]), "RANGE" + str(type(p[1])))

def p_ident(p):
    'ident : IDENT'
    p[0] = Node(p[1], 'ident')

def p_value(p):
    """value :   NUMBER
             |   STRING"""
    p[0] = Node(p[1], type(p[1]))

# Error rule for syntax errors
def p_error(p):
    print "Syntax error in input!"

parser = yacc.yacc()

result = parser.parse(squer2)
print result