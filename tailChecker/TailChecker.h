#ifndef TAILCHECKER_H
#define TAILCHECKER_H

#include <string>
using std::string;

struct TextLocation {
   int line;        
   int scol, ecol;  
   TextLocation( int l, int s, int e ) : line(l), scol(s), ecol(e) {}   
   TextLocation with( TextLocation ll ) const {
      return TextLocation( line, scol, ll.ecol );
   }
};

struct Error {
   TextLocation loc;
   string msg;
   Error( TextLocation l, string s );
};

struct Token {
   typedef enum { 
      TYPEDEF, VARDECL,BASIC, PRODUCT, SUM, SEQUENCE, FUNCTION, SUBTYPE,
      LP, RP,ACCESSP, TOP, MKPROD,TREATAS, EMBED,HEAD, TAIL, CONS, ID, NL, TEOF 
   } 
   TokType;
   TokType tok_type;
   static string tok_class( TokType );
   string value;
   TextLocation loc;
   Token( TokType t, string v, int l, int s, int e )
      : tok_type(t), value(v), loc(l,s,e) {}
   string what() const {
      return tok_class( tok_type ) + " \"" + value + "\"";
   }
};

//=================================

#include <vector>
using std::vector;
extern vector<string> file;
void highlight( TextLocation l );
string err_intro( Token );
   
class Lexer {
   int line, col;     
   char c() const { return file[line][col]; }
public:
   Lexer( string filename );
   Token tok();                  
   Token tok( Token::TokType ); 
   Token peek();                 
};

//=================================

#include <iostream>
#include <map>
using std::map;
#include <vector>
using std::vector;
#include <set>
using std::set;
#include <utility>
using std::pair;

class Type;

extern Token   TOPToken;
void  add_type( Type* );
Type* lookup_type( Token name );        
void  add_var( Token, Type* );
extern map<string,Type*> type_env;
extern map<string,Type*> var_env;

class TypeRef {
   mutable Type* ptr;
   Token name;
public:
   TypeRef( Token n ) : ptr(0), name(n) {}
   Token tok() const { return name; }
   bool valid() const { return lookup_type(name); }
   Type* type() const { 
      ptr = lookup_type(name); 
      if(!ptr) throw "fatal type error"; 
      return ptr;
   }
};
   
class Type {
   Token name_;
protected:
   TypeRef super;
public:
   typedef enum { 
      BASIC, PRODUCT, SUM, SEQUENCE, FUNCTION, SUBTYPE
   } Kind;
   Type( Token n, Token s ) : name_(n), super(s) {}
   virtual Kind kind() const = 0;
   Token name() const { return name_; }
   bool is_subtype_of( Type* t ) const;  
   virtual ~Type() {}
};

struct Basic : public Type {
   Basic( Token n ) : Type(n,TOPToken) {}
   Kind kind() const { return BASIC; }   
};

typedef vector<pair<Token,TypeRef> > FieldEnv;

class Product : public Type {
   FieldEnv fields;
public:   
   Product( Token n, FieldEnv v ) : Type(n,TOPToken), fields(v) {}
   Kind kind() const { return PRODUCT; }
   Type* field( string field_name ) const;
   set<string> field_names() const;  
};
   
class Sum : public Type {
   FieldEnv fields;
public: 
   Sum( Token n, FieldEnv v ) : Type(n,TOPToken), fields(v) {}
   Kind kind() const { return SUM; }
   Type* field( string field_name ) const;
   set<string> field_names() const;  
};
   
class Sequence : public Type {
   TypeRef contained;   
public:   
   Sequence( Token n, Token t ) : Type(n,TOPToken), contained(t) {}
   Kind kind() const { return SEQUENCE; }
   Type* contained_type() const { return contained.type(); }   
};
   

class Function : public Type {
   TypeRef arg, result;
public:   
   Function( Token n, Token a, Token r ): Type(n,TOPToken), arg(a), result(r) {}
   Kind kind() const { return FUNCTION; }
   Type* arg_type() const { return arg.type(); }
   Type* result_type() const { return result.type(); }   
};
   
class Subtype : public Type {
   FieldEnv fields;
public:   
   Subtype( Token n, Token s, FieldEnv v ) : Type(n,s), fields(v) {}
   Kind kind() const { return SUBTYPE; }
   Type* field( string field_name ) const;
   set<string> field_names() const;    
};
   
//========================================

#include <map>
#include <vector>

struct Exp {
   virtual Type* type() const = 0;
   virtual TextLocation loc() const = 0;
   virtual ~Exp() {}
};

extern vector<Exp*> exprs;

class Var : public Exp {
   Token name;
public:
   Var( Token n ) : name(n) {}
   Type* type() const;
   TextLocation loc() const { return name.loc; }
};

class FunCall : public Exp {
   Token name;
   Exp* arg;
public:
   FunCall( Token n, Exp* a ) : name(n), arg(a) {}
   Type* type() const;
   TextLocation loc() const { return name.loc.with(arg->loc()); }
};

class Accessp : public Exp {
   Token op;
   Exp* exp;
   Token field;
public:
   Accessp( Token o, Exp* e, Token f ) : op(o), exp(e), field(f) {}
   Type* type() const;
   TextLocation loc() const { return op.loc.with(field.loc); }
};

class Mkprod : public Exp {
   Token op;
   Token prod;
   vector<pair<Token,Exp*> > fields;
public:
   Mkprod( Token o, Token p, vector<pair<Token,Exp*> > f ) 
      : op(o), prod(p), fields(f) {}
   Type* type() const;
   TextLocation loc() const { return op.loc.with(fields.back().second->loc()); }
};

class Treatas : public Exp {
   Token op;
   Token sum;
   Token field;
   Exp* exp;
public:
   Treatas( Token o, Token s, Token f, Exp* e ) 
      : op(o), sum(s), field(f), exp(e) {}
   Type* type() const;
   TextLocation loc() const { return op.loc.with(exp->loc()); }
};

class Embed : public Exp {
   Token op;
   Token sum;
   Exp* exp;
public:
   Embed( Token o, Token s, Exp* e ) : op(o), sum(s), exp(e) {}
   Type* type() const;
   TextLocation loc() const { return op.loc.with(exp->loc()); }
};

class Head : public Exp {
   Token op;
   Exp* exp;
public:
   Head( Token o, Exp* e ) : op(o), exp(e) {}
   Type* type() const;
   TextLocation loc() const { return op.loc.with(exp->loc()); }
};

class Tail : public Exp {
   Token op;
   Exp* exp;
public:
   Tail( Token o, Exp* e ) : op(o), exp(e) {}
   Type* type() const;
   TextLocation loc() const { return op.loc.with(exp->loc()); }
};

class Cons : public Exp {
   Token op;
   Exp *head, *tail;
public:
   Cons( Token o, Exp* h, Exp* t ) : op(o), head(h), tail(t) {}
   Type* type() const;
   TextLocation loc() const { return op.loc.with(tail->loc()); }
};

class Parser {
   Lexer lexer;
   void parse_typedefs();
   void parse_vardecls();
   void parse_exprs();   
   void parse_typedef();
   FieldEnv parse_fields();
   Token parse_typename();
   void parse_vardecl();
   Exp* parse_expr();
   vector<pair<Token,Exp*> > parse_prod();
public:
   Parser( string filename ) : lexer(filename) {
      parse_typedefs();
      parse_vardecls();
      parse_exprs();
   }
};

#endif
