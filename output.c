//AUTO GENERATED CODE - DO NOT MODIFY - BEGIN
//Generated at: 2026-02-01T16:31:09.224526400Z
#ifndef __COMPILER_BUNDLE_K_SHARED_H__
#define __COMPILER_BUNDLE_K_SHARED_H__
#include<stdint.h>
#define true_k 1
#define false_k 0
#define persistent_k static
typedef int32_t int_t;typedef float float_t;typedef double double_t;typedef int8_t char_t;typedef int16_t short_t;typedef int64_t long_t;typedef uint8_t bool_t;typedef uint8_t ubyte_t;typedef uint32_t uint_t;typedef uint64_t ulong_t;typedef void unit_t;
#ifdef __GNUC__
#define immutable_k const
#define mutable_k __attribute__((unused))
#else
#define immutable_k const
#define mutable_k 
#endif
#endif
#include<stdio.h>
typedef int_t Int32;typedef long_t Int64;typedef short_t Int16;typedef char_t Int8;typedef float_t Float32;typedef double_t Float64;typedef bool_t Bool;typedef unit_t Unit;typedef uint_t UInt32;typedef ulong_t UInt64;typedef ubyte_t UInt8;Unit demoDefer(){printf("Starting function\n");printf("End of function\n");{printf("Second defer executed\n");printf("First defer executed\n");}}persistent_k mutable_k Int32 x=10;persistent_k unit_t __init_globals(void){while((x > 0)){printf("x = %d\n",x);(x)--;if((x == 5)){printf("Breaking the loop at x = %d\n",x);break;}}}int_t k_program_main(){__init_globals();return 0;}int_t main(){k_program_main();return 0;}
// Auto Generated -- END