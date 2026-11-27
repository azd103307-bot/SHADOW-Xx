#pragma once

#include "CoreMinimal.h"
#include "GameFramework/Pawn.h"
#include "GlowBallPawn.generated.h"

class USphereComponent;
class UStaticMeshComponent;
class USpringArmComponent;
class UCameraComponent;
class UPointLightComponent;

UCLASS()
class UNREALGLOWBALLS_API AGlowBallPawn : public APawn
{
    GENERATED_BODY()

public:
    AGlowBallPawn();

protected:
    virtual void BeginPlay() override;
    virtual void SetupPlayerInputComponent(UInputComponent* PlayerInputComponent) override;

    void MoveForward(float Value);
    void MoveRight(float Value);
    void JumpPressed();
    void JumpReleased();

    UPROPERTY(VisibleAnywhere, Category="Ball")
    TObjectPtr<USphereComponent> CollisionSphere;

    UPROPERTY(VisibleAnywhere, Category="Ball")
    TObjectPtr<UStaticMeshComponent> BallMesh;

    UPROPERTY(VisibleAnywhere, Category="Camera")
    TObjectPtr<USpringArmComponent> SpringArm;

    UPROPERTY(VisibleAnywhere, Category="Camera")
    TObjectPtr<UCameraComponent> FollowCamera;

    UPROPERTY(VisibleAnywhere, Category="Effects")
    TObjectPtr<UPointLightComponent> GlowLight;

    UPROPERTY(EditAnywhere, Category="Movement")
    float DriveForce = 180000.0f;

    UPROPERTY(EditAnywhere, Category="Movement")
    float JumpImpulse = 850000.0f;

    bool bCanJump = true;
};