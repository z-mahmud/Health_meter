package com.arlong.stepcounter;

/**
 * Created by Long on 7/4/2016.
 */
public class Madgwick {
    public float beta = 0.3f;								// 2 * proportional gain (Kp)
    public float[] q = new float[]{1.0f,0.0f,0.0f,0.0f};
    public float sampleFreq = 100f;
    public float[] MadgwickAHRSupdate(float gx, float gy, float gz, float ax, float ay, float az, float mx, float my, float mz) {
        float recipNorm;
        float s0, s1, s2, s3;
        float qDot1, qDot2, qDot3, qDot4;
        float hx, hy;
        float _2q0mx, _2q0my, _2q0mz, _2q1mx, _2bx, _2bz, _4bx, _4bz, _2q0, _2q1, _2q2, _2q3, _2q0q2, _2q2q3, q0q0, q0q1, q0q2, q0q3, q1q1, q1q2, q1q3, q2q2, q2q3, q3q3;

        // Use IMU algorithm if magnetometer measurement invalid (avoids NaN in magnetometer normalisation)
        if((mx == 0.0f) && (my == 0.0f) && (mz == 0.0f)) {
            q = MadgwickAHRSupdateIMU(gx, gy, gz, ax, ay, az);
            return q;
        }

        // Rate of change of quaternion from gyroscope
        qDot1 = 0.5f * (-q[1] * gx - q[2] * gy - q[3] * gz);
        qDot2 = 0.5f * (q[0] * gx + q[2] * gz - q[3] * gy);
        qDot3 = 0.5f * (q[0] * gy - q[1] * gz + q[3] * gx);
        qDot4 = 0.5f * (q[0] * gz + q[1] * gy - q[2] * gx);

        // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
        if(!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

            // Normalise accelerometer measurement
            recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // Normalise magnetometer measurement
            recipNorm = invSqrt(mx * mx + my * my + mz * mz);
            mx *= recipNorm;
            my *= recipNorm;
            mz *= recipNorm;

            // Auxiliary variables to avoid repeated arithmetic
            _2q0mx = 2.0f * q[0] * mx;
            _2q0my = 2.0f * q[0] * my;
            _2q0mz = 2.0f * q[0] * mz;
            _2q1mx = 2.0f * q[1] * mx;
            _2q0 = 2.0f * q[0];
            _2q1 = 2.0f * q[1];
            _2q2 = 2.0f * q[2];
            _2q3 = 2.0f * q[3];
            _2q0q2 = 2.0f * q[0] * q[2];
            _2q2q3 = 2.0f * q[2] * q[3];
            q0q0 = q[0] * q[0];
            q0q1 = q[0] * q[1];
            q0q2 = q[0] * q[2];
            q0q3 = q[0] * q[3];
            q1q1 = q[1] * q[1];
            q1q2 = q[1] * q[2];
            q1q3 = q[1] * q[3];
            q2q2 = q[2] * q[2];
            q2q3 = q[2] * q[3];
            q3q3 = q[3] * q[3];

            // Reference direction of Earth's magnetic field
            hx = mx * q0q0 - _2q0my * q[3] + _2q0mz * q[2] + mx * q1q1 + _2q1 * my * q[2] + _2q1 * mz * q[3] - mx * q2q2 - mx * q3q3;
            hy = _2q0mx * q[3] + my * q0q0 - _2q0mz * q[1] + _2q1mx * q[2] - my * q1q1 + my * q2q2 + _2q2 * mz * q[3] - my * q3q3;
            _2bx = (float)Math.sqrt(hx * hx + hy * hy);
            _2bz = -_2q0mx * q[2] + _2q0my * q[1] + mz * q0q0 + _2q1mx * q[3] - mz * q1q1 + _2q2 * my * q[3] - mz * q2q2 + mz * q3q3;
            _4bx = 2.0f * _2bx;
            _4bz = 2.0f * _2bz;

            // Gradient decent algorithm corrective step
            s0 = -_2q2 * (2.0f * q1q3 - _2q0q2 - ax) + _2q1 * (2.0f * q0q1 + _2q2q3 - ay) - _2bz * q[2] * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (-_2bx * q[3] + _2bz * q[1]) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + _2bx * q[2] * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
            s1 = _2q3 * (2.0f * q1q3 - _2q0q2 - ax) + _2q0 * (2.0f * q0q1 + _2q2q3 - ay) - 4.0f * q[1] * (1 - 2.0f * q1q1 - 2.0f * q2q2 - az) + _2bz * q[3] * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (_2bx * q[2] + _2bz * q[0]) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + (_2bx * q[3] - _4bz * q[1]) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
            s2 = -_2q0 * (2.0f * q1q3 - _2q0q2 - ax) + _2q3 * (2.0f * q0q1 + _2q2q3 - ay) - 4.0f * q[2] * (1 - 2.0f * q1q1 - 2.0f * q2q2 - az) + (-_4bx * q[2] - _2bz * q[0]) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (_2bx * q[1] + _2bz * q[3]) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + (_2bx * q[0] - _4bz * q[2]) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
            s3 = _2q1 * (2.0f * q1q3 - _2q0q2 - ax) + _2q2 * (2.0f * q0q1 + _2q2q3 - ay) + (-_4bx * q[3] + _2bz * q[1]) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (-_2bx * q[0] + _2bz * q[2]) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + _2bx * q[1] * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
            recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
            s0 *= recipNorm;
            s1 *= recipNorm;
            s2 *= recipNorm;
            s3 *= recipNorm;

            // Apply feedback step
            qDot1 -= beta * s0;
            qDot2 -= beta * s1;
            qDot3 -= beta * s2;
            qDot4 -= beta * s3;
        }

        // Integrate rate of change of quaternion to yield quaternion
        q[0] += qDot1 * (1.0f / sampleFreq);
        q[1] += qDot2 * (1.0f / sampleFreq);
        q[2] += qDot3 * (1.0f / sampleFreq);
        q[3] += qDot4 * (1.0f / sampleFreq);

        // Normalise quaternion
        recipNorm = invSqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        q[0] *= recipNorm;
        q[1] *= recipNorm;
        q[2] *= recipNorm;
        q[3] *= recipNorm;
        return q;
    }
    public float[] MadgwickAHRSupdateIMU(float gx, float gy, float gz, float ax, float ay, float az) {
        float recipNorm;
        float s0, s1, s2, s3;
        float qDot1, qDot2, qDot3, qDot4;
        float _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2 ,_8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

        // Rate of change of quaternion from gyroscope
        qDot1 = 0.5f * (-q[1] * gx - q[2] * gy - q[3] * gz);
        qDot2 = 0.5f * (q[0] * gx + q[2] * gz - q[3] * gy);
        qDot3 = 0.5f * (q[0] * gy - q[1] * gz + q[3] * gx);
        qDot4 = 0.5f * (q[0] * gz + q[1] * gy - q[2] * gx);

        // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
        if(!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

            // Normalise accelerometer measurement
            recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // Auxiliary variables to avoid repeated arithmetic
            _2q0 = 2.0f * q[0];
            _2q1 = 2.0f * q[1];
            _2q2 = 2.0f * q[2];
            _2q3 = 2.0f * q[3];
            _4q0 = 4.0f * q[0];
            _4q1 = 4.0f * q[1];
            _4q2 = 4.0f * q[2];
            _8q1 = 8.0f * q[1];
            _8q2 = 8.0f * q[2];
            q0q0 = q[0] * q[0];
            q1q1 = q[1] * q[1];
            q2q2 = q[2] * q[2];
            q3q3 = q[3] * q[3];

            // Gradient decent algorithm corrective step
            s0 = _4q0 * q2q2 + _2q2 * ax + _4q0 * q1q1 - _2q1 * ay;
            s1 = _4q1 * q3q3 - _2q3 * ax + 4.0f * q0q0 * q[1] - _2q0 * ay - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * az;
            s2 = 4.0f * q0q0 * q[2] + _2q0 * ax + _4q2 * q3q3 - _2q3 * ay - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * az;
            s3 = 4.0f * q1q1 * q[3] - _2q1 * ax + 4.0f * q2q2 * q[3] - _2q2 * ay;
            recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
            s0 *= recipNorm;
            s1 *= recipNorm;
            s2 *= recipNorm;
            s3 *= recipNorm;

            // Apply feedback step
            qDot1 -= beta * s0;
            qDot2 -= beta * s1;
            qDot3 -= beta * s2;
            qDot4 -= beta * s3;

        }

        // Integrate rate of change of quaternion to yield quaternion
        q[0] += qDot1 * (1.0f / sampleFreq);
        q[1] += qDot2 * (1.0f / sampleFreq);
        q[2] += qDot3 * (1.0f / sampleFreq);
        q[3] += qDot4 * (1.0f / sampleFreq);

        // Normalise quaternion
        recipNorm = invSqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        q[0] *= recipNorm;
        q[1] *= recipNorm;
        q[2] *= recipNorm;
        q[3] *= recipNorm;
        return q;
    }
    public float[] MadgwickAHRSupdateGyro(float gx, float gy, float gz) {
        float recipNorm;
        float qDot1, qDot2, qDot3, qDot4;

        // Rate of change of quaternion from gyroscope
        qDot1 = 0.5f * (-q[1] * gx - q[2] * gy - q[3] * gz);
        qDot2 = 0.5f * (q[0] * gx + q[2] * gz - q[3] * gy);
        qDot3 = 0.5f * (q[0] * gy - q[1] * gz + q[3] * gx);
        qDot4 = 0.5f * (q[0] * gz + q[1] * gy - q[2] * gx);

        // Integrate rate of change of quaternion to yield quaternion
        q[0] += qDot1 * (1.0f / sampleFreq);
        q[1] += qDot2 * (1.0f / sampleFreq);
        q[2] += qDot3 * (1.0f / sampleFreq);
        q[3] += qDot4 * (1.0f / sampleFreq);

        // Normalise quaternion
        recipNorm = invSqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        q[0] *= recipNorm;
        q[1] *= recipNorm;
        q[2] *= recipNorm;
        q[3] *= recipNorm;

        return q;
    }

//---------------------------------------------------------------------------------------------------
// Fast inverse square-root
// See: http://en.wikipedia.org/wiki/Fast_inverse_square_root

    public static float invSqrt(float x) {
        float xhalf = 0.5f*x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i>>1);
        x = Float.intBitsToFloat(i);
        x = x*(1.5f - xhalf*x*x);
        return x;
    }

    public float[][] quatern2rotMat(float[] q){   //dcm shi b2n
        float[][] R = new float[3][3];
        R[0][0]=q[0]*q[0]+q[1]*q[1]-q[2]*q[2]-q[3]*q[3];
        R[0][1]=2*(q[1]*q[2]+q[0]*q[3]);
        R[0][2]=2*(q[1]*q[3]-q[0]*q[2]);
        R[1][0]=2*(q[1]*q[2]-q[0]*q[3]);
        R[1][1]=q[0]*q[0]-q[1]*q[1]+q[2]*q[2]-q[3]*q[3];
        R[1][2]=2*(q[2]*q[3]+q[0]*q[1]);
        R[2][0]=2*(q[1]*q[3]+q[0]*q[2]);
        R[2][1]=2*(q[2]*q[3]-q[0]*q[1]);
        R[2][2]=q[0]*q[0]-q[1]*q[1]-q[2]*q[2]+q[3]*q[3];
        return R;
    }

    public float[] rotMat2euler(float[][] R){
        float eulerAngle[] = new float[3];//pitch, roll, heading;
        eulerAngle[0] = (float)Math.atan2(R[2][1],R[2][2]);
        eulerAngle[1] = -(float)Math.sin(R[2][0]);
        eulerAngle[2] = (float)Math.atan2(R[1][0],R[0][0]);
        return eulerAngle;
    }

    public float[][] euler2rotMat(float[] euler){   //dcm shi b2n
        float[][] R = new float[3][3];
        float phi = euler[0];
        float theta = euler[1];
        float psi = euler[2];
        R[0][0] = (float)(Math.cos(psi)*Math.cos(theta));
        R[0][1] = (float)(-Math.sin(psi)*Math.cos(phi) + Math.cos(psi)*Math.sin(theta)*Math.sin(phi));
        R[0][2] = (float)(Math.sin(psi)*Math.sin(phi) + Math.cos(psi)*Math.sin(theta)*Math.cos(phi));
        R[1][0] = (float)(Math.sin(psi)*Math.cos(theta));
        R[1][1] = (float)(Math.cos(psi)*Math.cos(phi) + Math.sin(psi)*Math.sin(theta)*Math.sin(phi));
        R[1][2] = (float)(-Math.cos(psi)*Math.sin(phi) + Math.sin(psi)*Math.sin(theta)*Math.cos(phi));
        R[2][0] = (float)-Math.sin(theta);
        R[2][1] = (float)(Math.cos(theta)*Math.sin(phi));
        R[2][2] = (float)(Math.cos(theta)*Math.cos(phi));
        return R;
    }

    public float[] quaternConj(float[] q){
        float[] qConj = new float[]{q[0], -q[1], -q[2], -q[3]};
        return qConj;
    }

    // Compute C = A * B
    private void Equal_AxB(float[][] matrix_A, float[][] matrix_B) {
        int m = matrix_A.length; // A rows
        int n = matrix_A[0].length; // A columns
        int p = matrix_B[0].length; // B columns

        float[][] matrix_C = new float[m][p];
        for(int i = 0; i < m; i++) { // Keep iterating until the last row of A
            for(int j = 0; j < p; j++) { // Keep iterating until the last column of B
                for(int k = 0; k < n; k++) { // Keep iterating until last column of A
                    matrix_C[i][j] += matrix_A[i][k] * matrix_B[k][j]; // Keep sum
                }
            }
        }
    }

    // A matrix M x N will have transpose N x M
    private void Transpose_A(float[][] matrix_A) {
        int m = matrix_A.length;
        int n = matrix_A[0].length;
        float[][] matrix_C = new float[n][m];
        for( int i = 0; i < m; i++ ) {
            for( int j = 0; j < n; j++ ) {
                matrix_C[j][i] = matrix_A[i][j];
            }
        }
    }

    public float[] quatern2euler(float[] q){
       return rotMat2euler(quatern2rotMat(q));
    }

    public float[] euler2quatern(float[] euler){
        float[] quatern = new float[4];
        float roll = euler[0];
        float pitch = euler[1];
        float yaw = euler[2];

        quatern[0] = (float)(Math.cos(roll/2) *Math.cos(pitch/2) *Math.cos(yaw/2) + Math.sin(roll/2) *Math.sin(pitch/2) *Math.sin(yaw/2));
        quatern[1] =(float)(Math.sin(roll/2) *Math.cos(pitch/2) *Math.cos(yaw/2) - Math.cos(roll/2) *Math.sin(pitch/2) *Math.sin(yaw/2));
        quatern[2] = (float)(Math.cos(roll/2) *Math.sin(pitch/2) *Math.cos(yaw/2) + Math.sin(roll/2) *Math.cos(pitch/2) *Math.sin(yaw/2));
        quatern[3] = (float)(Math.cos(roll/2) *Math.cos(pitch/2) *Math.sin(yaw/2) - Math.sin(roll/2) *Math.sin(pitch/2) *Math.cos(yaw/2));
        return quatern;
    }

    public float getBeta() {
        return beta;
    }

    public void setBeta(float beta) {
        this.beta = beta;
    }

    public float[] getQ() {
        return q;
    }

    public void setQ(float[] q) {
        this.q = q;
    }

    public float getSampleFreq() {
        return sampleFreq;
    }

    public void setSampleFreq(float sampleFreq) {
        this.sampleFreq = sampleFreq;
    }
}
