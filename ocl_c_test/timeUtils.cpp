/*
 * OpenCL_facade.cpp
 *
 *  Created on: 04.01.2011
 *      Author: christof
 */

#include "timeUtils.hpp"
#include <iostream>

using namespace timeUtils;

Clock::Clock()
{
  this->hasStarted = false;
  this->isStopped = false;
}

void
Clock::reset()
{
  this->hasStarted = false;
  this->isStopped = false;
}

void
Clock::start()
{
  gettimeofday(&this->tStart, NULL);
  this->hasStarted = true;
}

void
Clock::stop()
{
  gettimeofday(&this->tEnd, NULL);
  this->isStopped = true;
}

double
Clock::getTimeInSeconds()
{
  if (this->hasStarted && this->isStopped)
    return double(this->tEnd.tv_sec) - double(this->tStart.tv_sec) + 1e-6
        * (double(this->tEnd.tv_usec) - double(this->tStart.tv_usec));
  else
    return -1;

}

