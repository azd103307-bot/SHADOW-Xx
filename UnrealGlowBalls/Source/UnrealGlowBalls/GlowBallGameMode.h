#pragma once
#include "CoreMinimal.h"
#include "GameFramework/GameModeBase.h"
#include "GlowBallGameMode.generated.h"

UCLASS()
class UNREALGLOWBALLS_API AGlowBallGameMode : public AGameModeBase
{
    GENERATED_BODY()
public:
    AGlowBallGameMode();
    virtual void BeginPlay() override;
    void RegisterOrbCollected();

private:
    int32 CollectedOrbs = 0;
    int32 TotalOrbs = 12;
};