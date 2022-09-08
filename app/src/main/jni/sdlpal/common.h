//
// Copyright (c) 2009, Wei Mingzhi <whistler_wmz@users.sf.net>.
// All rights reserved.
//
// This file is part of SDLPAL.
//
// SDLPAL is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

#ifndef _COMMON_H
#define _COMMON_H

//#define PAL_WIN95          1 // not valid for now
#define PAL_CLASSIC        1

#ifdef __cplusplus
extern "C"
{
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <limits.h>
#include <stdarg.h>
#include <assert.h>

#include "SDL.h"
#include "SDL_endian.h"
#include "SDL_compat.h"

#ifdef _SDL_stdinc_h
#define malloc       SDL_malloc
#define calloc       SDL_calloc
#define free         SDL_free
#define realloc      SDL_realloc
#endif

#if SDL_BYTEORDER == SDL_LIL_ENDIAN
#define SWAP16(X)    (X)
#define SWAP32(X)    (X)
#else
#define SWAP16(X)    SDL_Swap16(X)
#define SWAP32(X)    SDL_Swap32(X)
#endif

#ifndef max
#define max(a, b)    (((a) > (b)) ? (a) : (b))
#endif

#ifndef min
#define min(a, b)    (((a) < (b)) ? (a) : (b))
#endif

#if defined (__SYMBIAN32__)
#undef  _WIN32
#undef  SDL_INIT_JOYSTICK
#define SDL_INIT_JOYSTICK     0
#define PAL_HAS_MOUSE         1

#elif defined (GEKKO)
#define PAL_HAS_JOYSTICKS     1
#define PAL_HAS_MOUSE         0

#elif defined (PSP)
#define PAL_HAS_JOYSTICKS     0

#elif defined (ANDROID)
#undef  _WIN32
#undef  SDL_INIT_JOYSTICK
#define SDL_INIT_JOYSTICK     1
#define PAL_HAS_MOUSE         1

#elif defined __IPHONEOS__
#undef SDL_INIT_JOYSTICK
#define SDL_INIT_JOYSTICK     0
#define PAL_HAS_MOUSE         1
#else

#define PAL_HAS_MOUSE         1
#define PAL_HAS_JOYSTICKS     1
#ifndef _WIN32_WCE
#define PAL_ALLOW_KEYREPEAT   1

#if !defined (CYGWIN) && !defined (DINGOO) && !defined (GPH) && !defined (GEKKO)
#define PAL_HAS_MP3           1
#endif
#endif

#endif

#ifndef SDL_INIT_CDROM
#define SDL_INIT_CDROM        0
#endif

#ifdef _WIN32
#include <windows.h>

#if !defined(__BORLANDC__) && !defined(_WIN32_WCE)
#include <io.h>
#endif

#define vsnprintf _vsnprintf
#define snprintf _snprintf

#ifdef _MSC_VER
#pragma warning (disable:4018)
#pragma warning (disable:4028)
#pragma warning (disable:4244)
#pragma warning (disable:4305)
#pragma warning (disable:4761)
#pragma warning (disable:4996)
#endif

#ifndef _LPCBYTE_DEFINED
#define _LPCBYTE_DEFINED
typedef const BYTE *LPCBYTE;
#endif

#define PAL_HAS_NATIVEMIDI  1

#else

#include <unistd.h>

#define CONST               const
#ifndef FALSE
#define FALSE               0
#endif
#ifndef TRUE
#define TRUE                1
#endif
#define VOID                void
typedef char                CHAR;
typedef short               SHORT;
typedef long                LONG;

typedef unsigned long       ULONG, *PULONG;
typedef unsigned short      USHORT, *PUSHORT;
typedef unsigned char       UCHAR, *PUCHAR;

typedef unsigned short      WORD, *LPWORD;
typedef unsigned int        DWORD, *LPDWORD;
typedef int                 INT, *LPINT;
typedef unsigned int        UINT, *PUINT, UINT32, *PUINT32;
typedef unsigned char       BYTE, *LPBYTE;
typedef CONST BYTE         *LPCBYTE;
typedef float               FLOAT, *LPFLOAT;
typedef void               *LPVOID;
typedef const void         *LPCVOID;
typedef CHAR               *LPSTR;
typedef const CHAR         *LPCSTR;
    
#ifndef __OBJC__
    typedef int            BOOL, *LPBOOL;
#endif

#endif

#if defined (__SYMBIAN32__) || defined ( ANDROID )
#define PAL_LARGE           static
#else
#define PAL_LARGE           /* */
#endif


#define SCREEN_TYPE_SMALL 0
#define SCREEN_TYPE_MIDDLE 1
#define SCREEN_TYPE_LARGE 2
#define SCREEN_TYPE SCREEN_TYPE_SMALL

#ifdef __cplusplus
}
#endif

#endif
