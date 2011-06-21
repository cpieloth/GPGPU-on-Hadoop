/**
 * @file
 * @brief       Stoppuhr
 * @author      Christof
 */

#ifndef TIMEUTILS_HPP_
#define TIMEUTILS_HPP_

#include <sys/time.h>

namespace timeUtils
{
  class Clock
  {
  public:
    Clock();
    void
    start();
    void
    stop();
    double
    getTimeInSeconds();
    void
    reset();

  private:
    timeval tStart;
    timeval tEnd;
    bool hasStarted;
    bool isStopped;
  };
}

#endif /* TIMEUTILS_HPP_ */
