#include "GlowBallGameMode.h"
#include "GlowBallPawn.h"

AGlowBallGameMode::AGlowBallGameMode()
{
    DefaultPawnClass = AGlowBallPawn::StaticClass();
}