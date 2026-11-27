#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Actor.h"
#include "GlowOrbPickup.generated.h"

class USphereComponent;
class UPrimitiveComponent;
class UStaticMeshComponent;
class UPointLightComponent;

UCLASS()
class UNREALGLOWBALLS_API AGlowOrbPickup : public AActor
{
    GENERATED_BODY()

public:
    AGlowOrbPickup();

protected:
    virtual void BeginPlay() override;

    UPROPERTY(VisibleAnywhere, Category="Pickup")
    TObjectPtr<USphereComponent> Trigger;

    UPROPERTY(VisibleAnywhere, Category="Pickup")
    TObjectPtr<UStaticMeshComponent> OrbMesh;

    UPROPERTY(VisibleAnywhere, Category="Effects")
    TObjectPtr<UPointLightComponent> OrbLight;

    UFUNCTION()
    void HandleOverlap(UPrimitiveComponent* OverlappedComponent, AActor* OtherActor,
        UPrimitiveComponent* OtherComponent, int32 OtherBodyIndex, bool bFromSweep,
        const FHitResult& SweepResult);
};