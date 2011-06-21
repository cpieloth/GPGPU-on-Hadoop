/**
 * @file
 * @brief       Einfaches loggen auf sto
 * @author      Christof
 */

#ifndef LOGGING_HPP_
#define LOGGING_HPP_

#include <vector>
#include <ostream>
#include <string>
#include <sstream>

namespace logging
{
  class Level
  {
  public:
    Level(std::string name, unsigned short value) :
      NAME(name), VALUE(value)
    {
    }
    ;
    const std::string NAME;
    const unsigned short VALUE;

    static const Level ALL;
    static const Level NORMAL;

    static const Level ERR;
    static const Level WARN;
    static const Level INFO;
    static const Level TRACE;
    static const Level DEBUG;

  };

  class Logger
  {
  public:
    static void
    setLogMask(Level);
    static void
    setLogMask(std::vector<Level>);
    static void
    clearLogMask();
    static unsigned short
    getLogMask();
    static void
    log(std::string, Level, std::basic_ostream<char>&);
    static void
    logError(std::string, std::basic_ostream<char>&);
    static void
    logWarn(std::string, std::basic_ostream<char>&);
    static void
    logInfo(std::string, std::basic_ostream<char>&);
    static void
    logTrace(std::string, std::basic_ostream<char>&);
    static void
    logDebug(std::string, std::basic_ostream<char>&);
    static
    std::stringstream sStream;

  private:
    static unsigned short logMask;

  };

}
#endif /* LOGGING_HPP_ */
