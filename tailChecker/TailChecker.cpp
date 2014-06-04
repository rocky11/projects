#include <iostream>
using std::cout;
using std::endl;
#include "TailChecker.h"

vector<Exp*>::iterator p1;

void exprcheck() {	
	for( p1 = exprs.begin(); p1 != exprs.end(); ++p1)
		cout <<(*p1)->type()->name().value <<endl;		
}

void frontexpr(){
	exprs.erase(exprs.begin(), ++p1);
	exprcheck();
}

int main( int argc, char *argv[] ) {
   if( argc != 2 ) {
      cout << "Usage: " << argv[0] << " <Input file>" << endl;
      exit(1);
   }       
      Parser p( argv[1] );
      (void) p;       
	  exprcheck();
	  
	return 0;
}

Error::Error( TextLocation l, string s ) : loc(l), msg(s) {
      cout << "\nError [line: " << (loc.line+1) <<"]"<< endl<< msg << endl;
      highlight( loc );
	  frontexpr();
}
//=========================
#include <fstream>
using std::ifstream;
#include <iostream>
using std::cout;
using std::endl;
#include <sstream>
using std::ostringstream;

vector<string> file;
//=========================
string ncopies( char c, int n ) {
   string s;
   while( n ) {
      s += c;
      --n;
   }
   return s;
}

void highlight( TextLocation l ) {
   ostringstream oss;
   oss << l.line+1;
   string ln = oss.str();
   cout << ln << ":" << file[l.line];  
   cout << ncopies(' ',l.scol+ln.length()+1) 
        << ncopies('^',l.ecol-l.scol+1) << endl;
}

Lexer::Lexer( string filename ) : line(0), col(0) {
   ifstream fin( filename.c_str() );   
   
   if(!fin) {
		cout<<"\nThe Input file could not be opened.\n"<<endl;
		exit (1);
	}

   string s;
   while (! fin.eof() )	{
		getline(fin, s );
		if( !fin ) 
			break;
		s += '\n';
		file.push_back(s);
   } 

   file.push_back("$");
}
   
bool is_ws( char c ) { return c==' ' || c=='\t'; }

bool is_id_char( char c ) {
   if( c>='a' && c <='z' ) return true;
   if( c>='A' && c <='Z' ) return true;
   return c=='_';
}

Token check( string s, int l, int sc, int ec ) {
   if( s == "TYPEDEF" )
      return Token( Token::TYPEDEF, s, l, sc, ec );
   if( s == "VARDECL" )
      return Token( Token::VARDECL, s, l, sc, ec );
   if( s == "TOP" )
      return Token( Token::TOP, s, l, sc, ec );
   if( s == "BASIC" )
      return Token( Token::BASIC, s, l, sc, ec );
   if( s == "PRODUCT" )
      return Token( Token::PRODUCT, s, l, sc, ec );
   if( s == "SUM" )
      return Token( Token::SUM, s, l, sc, ec );
   if( s == "SEQUENCE" )
      return Token( Token::SEQUENCE, s, l, sc, ec );   
   if( s == "FUNCTION" )
      return Token( Token::FUNCTION, s, l, sc, ec );
   if( s == "SUBTYPE" )
      return Token( Token::SUBTYPE, s, l, sc, ec );
   if( s == "ACCESSP" )
      return Token( Token::ACCESSP, s, l, sc, ec );
   if( s == "MKPROD" )
      return Token( Token::MKPROD, s, l, sc, ec );
   if( s == "TREATAS" )
      return Token( Token::TREATAS, s, l, sc, ec );
   if( s == "EMBED" )
      return Token( Token::EMBED, s, l, sc, ec );
   if( s == "HEAD" )
      return Token( Token::HEAD, s, l, sc, ec );
   if( s == "TAIL" )
      return Token( Token::TAIL, s, l, sc, ec );
   if( s == "CONS" )
      return Token( Token::CONS, s, l, sc, ec );   
   return Token( Token::ID, s, l, sc, ec );
}

Token Lexer::tok() {
   if( line == file.size()-1 )
      return Token( Token::TEOF, "$", line, col, col );

   while( is_ws( c() ) )
      ++col;

   if( c()=='#' ||  c()=='\n' ) {
      Token t( Token::NL, "\n", line, col, col );
      ++line;
      col=0;
      return t;
   }
   if( c()=='(' ) {
      Token t( Token::LP, "(", line, col, col );
      ++col;
      return t;
   }
   if( c()==')' ) {
      Token t( Token::RP, ")", line, col, col );
      ++col;
      return t;
   }  
   string id;
   int start_col = col;
   while( is_id_char( c() ) ) {
      id += c();
      ++col;
   }
   int end_col = col-1;
   return check( id, line, start_col, end_col );
}

Token Lexer::tok( Token::TokType tt ) {
   Token t = tok();   
   return t;
}

Token Lexer::peek() {
   int l = line, c = col;
   Token t = tok();
   line = l;
   col = c;
   return t;
}

//===========================
#include <sstream>
using std::ostringstream;
//===========================
vector<Exp*> exprs;

bool is_typename( Token::TokType tt ) {
   return tt==Token::ID || tt==Token::TOP;
}

string err_intro( Token t ) {
   ostringstream oss;   
   return oss.str();
}

void Parser::parse_typedefs() {
   while( lexer.peek().tok_type == Token::NL )
      lexer.tok();
   parse_typedef();
   while( lexer.peek().tok_type == Token::TYPEDEF )
      parse_typedef();
   lexer.tok( Token::NL );
}

void Parser::parse_typedef() {
   lexer.tok( Token::TYPEDEF );
   Token id = lexer.tok( Token::ID );
   Token kind = lexer.tok();
   switch( kind.tok_type ) {
      case Token::BASIC:
         {
            add_type( new Basic(id) );
            break;
         }
      case Token::PRODUCT:
         {
            FieldEnv fields = parse_fields();
            add_type( new Product(id,fields) );
            break;
         }
      case Token::SUM:
         {
            FieldEnv fields = parse_fields();
            add_type( new Sum(id,fields) );
            break;
         }
      case Token::SEQUENCE:
         {
            Token type = parse_typename();
            add_type( new Sequence(id,type) );
            break;
         }     
      case Token::FUNCTION:
         {
            Token arg = parse_typename();
            Token res = parse_typename();
            add_type( new Function(id,arg,res) );
            break;
         }
      case Token::SUBTYPE:
         {
            Token type = parse_typename();
            FieldEnv fields = parse_fields();
            add_type( new Subtype(id,type,fields) );
            break;
         }     
   }
   lexer.tok( Token::NL );
}

FieldEnv Parser::parse_fields() {
   Token id = lexer.tok( Token::ID );
   Token type = parse_typename();
   FieldEnv result;
   if( lexer.peek().tok_type == Token::ID )
      result = parse_fields();
   result.push_back( std::make_pair(id,TypeRef(type)) );
   return result;
}

Token Parser::parse_typename() {
   Token type = lexer.tok();   
   return type;
}

void Parser::parse_vardecls() {
   parse_vardecl();
   while( lexer.peek().tok_type == Token::VARDECL )
      parse_vardecl();
   lexer.tok( Token::NL );
}

void Parser::parse_vardecl() {
   lexer.tok( Token::VARDECL );
   Token id = lexer.tok( Token::ID );
   Token type = parse_typename();
   Type* t = lookup_type( type ); 
   add_var( id, t );
   lexer.tok( Token::NL );
}

void Parser::parse_exprs() {
   Exp* e = parse_expr();
   lexer.tok( Token::NL );
   exprs.push_back(e);
   if( lexer.peek().tok_type != Token::TEOF )
      parse_exprs();
}
   
Exp* Parser::parse_expr() {
   Token t = lexer.tok();
   
   if( t.tok_type == Token::ID ) {
      return new Var(t);
   }
   if( t.tok_type == Token::LP ) {
      Token op = lexer.tok();
      switch( op.tok_type ) {
         case Token::ID: {
            Exp* e = parse_expr();
            lexer.tok( Token::RP );
            return new FunCall( op, e );
         }
         case Token::ACCESSP: {
            Exp* e = parse_expr();
            Token fn = lexer.tok( Token::ID );
            lexer.tok( Token::RP );
            return new Accessp( op, e, fn );
         }
         case Token::MKPROD: {
            Token pn = lexer.tok( Token::ID );
            vector<pair<Token,Exp*> > v = parse_prod();
            lexer.tok( Token::RP );
            return new Mkprod( op, pn, v );
         }
         case Token::TREATAS: {
            Token s = lexer.tok( Token::ID );
            Token f = lexer.tok( Token::ID );
            Exp* e = parse_expr();
            lexer.tok( Token::RP );
            return new Treatas( op, s, f, e );
         }
         case Token::EMBED: {
            Token s = lexer.tok( Token::ID );
            Exp* e = parse_expr();
            lexer.tok( Token::RP );
            return new Embed( op, s, e );
         }
         case Token::HEAD: {
            Exp* e = parse_expr();
            lexer.tok( Token::RP );
            return new Head( op, e );
         }
         case Token::TAIL: {
            Exp* e = parse_expr();
            lexer.tok( Token::RP );
            return new Tail( op, e );
         }
         case Token::CONS: {
            Exp* e = parse_expr();
            Exp* f = parse_expr();
            lexer.tok( Token::RP );
            return new Cons( op, e, f );
         }        
      }
   }   
}

vector<pair<Token,Exp*> > Parser::parse_prod() {
   vector<pair<Token,Exp*> > v;
   Token t = lexer.tok( Token::ID );
   Exp* e = parse_expr();
   if( lexer.peek().tok_type == Token::ID )
      v = parse_prod();
   v.push_back( std::make_pair( t, e ) );
   return v;
}

string Token::tok_class( Token::TokType tt ) {
   switch( tt ) {
      case TYPEDEF: 
      case VARDECL:      
      case BASIC: 
      case PRODUCT: 
      case SUM: 
      case SEQUENCE:      
      case FUNCTION: 
      case SUBTYPE:
      case ACCESSP: 
      case MKPROD: 
      case TREATAS: 
      case EMBED:
      case HEAD: 
      case TAIL: 
      case CONS:      
      case LP: 
      case RP:
         return "punctuation";
      case ID: 
         return "identifier";
      case NL: 
         return "newline";
      case TEOF:
         return "end of file marker"; 
	}
}

//=================================
#include <algorithm>
#include <sstream>
using std::ostringstream;
//=================================
string ei( Token t ) {
   ostringstream oss;   
   return oss.str();
}

Type* lookup_var( Token t ) {
   string vn = t.value;
   map<string,Type*>::iterator i = var_env.find( vn );
   if( i == var_env.end() ) {
      Error( t.loc, ei(t)+"Reference to \""+vn+"\" without declaration" );	  
   } 
   else
	   return i->second;
}

Type* Var::type() const {
   return lookup_var( name );
}

Type* FunCall::type() const {
   Type* fun = lookup_var( name );
   if( Function* f = dynamic_cast<Function*>(fun) ) {
      if( !arg->type()->is_subtype_of( f->arg_type() ) ) {
         ostringstream oss;
         //oss << ei(name) << "While calling '"<< name.value<<"' detected type mismatch: '"<< arg->type()->name().value<< "' is not a subtype of \""<< f->arg_type()->name().value;
         oss << ei(name) << "In call to function \"" << name.value
             << "\"\nType mismatch: actual parameter with type \""
             << arg->type()->name().value 
             << "\"\nis not a subtype of formal parameter type \""
             << f->arg_type()->name().value;

		 throw Error( loc(), oss.str() );		 
      }
      return f->result_type();
   }
   else { 
      throw Error( name.loc, ei(name)+"Tried to use the variable \""+name.value+"\" as a function");	  
	}
}

   
Type* Accessp::type() const {
   Type* et = exp->type();
   Type* result;
   if( Product* p = dynamic_cast<Product*>(et) ) {
      result = p->field( field.value );	 
   }
   else if( Subtype* p = dynamic_cast<Subtype*>(et) ) {
      result = p->field( field.value );	  
   }
   else {
      Error( exp->loc(), ei(op)+"Tried to call ACCESSP with type \""+et->name().value+"\"" );	  
   }
   if( !result ) {
      ostringstream oss;
      oss << ei(op) << "Tried calling ACCESSP with field '"<< field.value<<"' from an expression of type '" << et->name().value<<"'";	  
      Error( exp->loc(), oss.str() );	 
   }   
}

Type* get_type( Token t ) {
   Type* p = lookup_type( t );
   if( !p ) {
      Error( t.loc, ei(t)+"Referenced undeclared type \""+t.value+"\"" );	  
   }
   return p;
}

Type* Mkprod::type() const {
   Type* pt = get_type(prod);
   if( Product* p = dynamic_cast<Product*>(pt) ) {
      set<string> fns = p->field_names();
      set<string> mfns;
      vector<pair<Token,Exp*> >::const_iterator i;
      for( i=fields.begin(); i!=fields.end(); ++i ) {
         string fn = i->first.value;
         if( mfns.count(fn) ) {
            throw Error( i->second->loc(), ei(i->first)+"Inside MKPROD you have referenced field \""+fn+"\" more than once" );			
         }
         mfns.insert( fn );
         if( !i->second->type()->is_subtype_of( p->field( fn ) ) ) {
            throw Error( i->second->loc(), ei(i->first) +"Type-mismatch in MKPROD:\""+fn+"\" has type \""+p->field(fn)->name().value+"\"\nexpression has type \""+i->second->type()->name().value+"\":" );			
         }
      }
      set<string> diff;
      set_difference( fns.begin(), fns.end(), mfns.begin(), mfns.end(),
                      inserter(diff,diff.begin()) );
      if( diff.size() ) {
         ostringstream oss;
         oss << "While calling MKPROD, following fields found not initialized:\n";
         for( set<string>::iterator i=diff.begin(); i!=diff.end(); ++i )
            oss << "   " << *i << "\n";
         throw Error( loc(), oss.str() );		 
      }
      return p;
   }
   else {
      throw Error( prod.loc, ei(op)+"Tried to call MKPROD on type \""+prod.value+"\"" );	  
   }
}
   
Type* Treatas::type() const {
   Type* st = get_type(sum);
   if( exp->type() != st ) {
      Error( exp->loc(), ei(field)+"Expression type \""+exp->type()->name().value+"\" does not match type of first argument to TREATAS" );	  
   }
   if( Sum* s = dynamic_cast<Sum*>(st) ) {
      Type* ft = s->field( field.value );
      if( !ft ) {
         Error( field.loc, ei(field)+"Field \""+field.value+"\" doesn't exist in SUM type \""+sum.value+"\"" );		 
      }
	  return ft;      
   }
   else {
      Error( sum.loc, ei(sum) +"TREATAS's 1st argument must be of SUM type" );	  
   }
}

Type* Embed::type() const {
   Type* st = get_type(sum);
   if( Sum* s = dynamic_cast<Sum*>(st) ) {
      set<string> fns = s->field_names();
      Type* et = exp->type();
      for( set<string>::iterator i=fns.begin(); i!=fns.end(); ++i )
         if( et->is_subtype_of( s->field(*i) ) )
            return st;
      Error( exp->loc(), ei(sum)+"Expression has type \""+et->name().value+"\"that is not in SUM type \""+sum.value+"\"" );	  
   }
   else {
      Error( sum.loc, ei(sum)+"First argument of EMBED must be the name of a SUM type" );	  
   }
}
   
Type* Head::type() const {
   Type* et = exp->type();
   if( Sequence* s = dynamic_cast<Sequence*>(et) ) {
      return s->contained_type();
   }
   else {
      Error( exp->loc(), ei(op) +"HEAD's argument must have sequence type, but found type \""+et->name().value+"\"" );	  
   }
}
   
Type* Tail::type() const {
   Type* et = exp->type();
   if( Sequence* s = dynamic_cast<Sequence*>(et) ) {
      return s;
   }
   else {
      Error( exp->loc(), ei(op)+"TAIL's argument must have sequence type, but found type \""+et->name().value+"\"" );	  
   }
}
   
Type* Cons::type() const {
   Type* et = tail->type();
   if( Sequence* s = dynamic_cast<Sequence*>(et) ) {
      if( head->type()->is_subtype_of( s->contained_type() ) )
         return s;
      Error( head->loc(), ei(op)+"Type-mismatch in CONS "+"First arg has type \""+head->type()->name().value+"\" that is not a subtype of \""+s->contained_type()->name().value
         +"\",\nthat is in the sequence's argument" );
   }
   else {
      Error( tail->loc(), ei(op) +"Second argument in CON must have sequence type, but found type \""+et->name().value+"\"" );	  
   }
   return et;
}

//**************************//

#include <iostream>
#include <set>
using std::set;
#include <sstream>
using std::ostringstream;

Token   TOPToken = Token( Token::TOP, "TOP", 0, 0, 0 );
map<string,Type*> type_env;
map<string,Type*> var_env;

bool Type::is_subtype_of( Type* t ) const {
   if( this==t ) return true;
   if( this==lookup_type(TOPToken) ) return false;
   return super.type()->is_subtype_of(t);
}

void  add_type( Type* t ) {
   string name = t->name().value;
   type_env.insert( make_pair(name,t) );   
}

Type* lookup_type( Token t ) {
   string name = t.value;
   if( type_env.count( name ) ) 
      return type_env.find(name)->second;
   return 0;
}

void  add_var( Token t, Type* type ) {
   string name = t.value;
   var_env.insert( make_pair(name,type) );   
}

Type* lookup_field_type( string fn, const FieldEnv& fields ) {
   for( FieldEnv::const_iterator i=fields.begin(); i!=fields.end(); ++i ) {
      if( fn == i->first.value )
         return i->second.type();
   }
   return 0;
}

Type* Product::field( string fn ) const {
   return lookup_field_type( fn, fields );
}

Type* Sum::field( string fn ) const {
   return lookup_field_type( fn, fields );
}

Type* Subtype::field( string fn ) const {
   Type* t = lookup_field_type( fn, fields );
   if( t )
      return t;
   return dynamic_cast<Product*>(super.type())->field(fn);
}

set<string> Product::field_names() const {
   set<string> s;
   for( FieldEnv::const_iterator i=fields.begin(); i!=fields.end(); ++i )
      s.insert( i->first.value );
   return s;
}

set<string> Sum::field_names() const {
   set<string> s;
   for( FieldEnv::const_iterator i=fields.begin(); i!=fields.end(); ++i )
      s.insert( i->first.value );
   return s;
}

set<string> Subtype::field_names() const {
   set<string> s = dynamic_cast<Product*>(super.type())->field_names();
   for( FieldEnv::const_iterator i=fields.begin(); i!=fields.end(); ++i )
      s.insert( i->first.value );
   return s;
}
