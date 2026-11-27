#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Actor.h"
#include "GlowHazardBall.generated.h"

class USphereComponent;
class UStaticMeshComponent;
class UPointLightComponent;

UCLASS()
class UNREALGLOWBALLS_API AGlowHazardBall : public AActor
{
    GENERATED_BODY()

public:
    AGlowHazardBall();
    void Launch(FLinearColor Color, const FVector& Impulse);

private:
    UPROPERTY(VisibleAnywhere, Category="Ball")
    TObjectPtr<USphereComponent> PhysicsBody;

    UPROPERTY(VisibleAnywhere, Category="Ball")
    TObjectPtr<UStaticMeshComponent> BallMesh;

    UPROPERTY(VisibleAnywhere, Category="Effects")
    TObjectPtr<UPointLightComponent> BallLight;
};