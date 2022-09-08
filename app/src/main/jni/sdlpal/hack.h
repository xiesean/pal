#pragma once
#include "common.h"

#ifdef __cplusplus
extern "C"
{
#endif

// ��ǽ
void setFlyMode(BOOL isFly);
BOOL isFlyMode();

void setNoEnemy(BOOL isTrans);
BOOL isNoEnemy();

// ����
void uplevel();

// show me the money
void addMoney();
void addExp(int value);

void addMoney2(int value);


#ifdef __cplusplus
}
#endif