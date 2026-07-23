import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Grid,
  Chip,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Divider,
  Alert,
  AlertTitle,
  IconButton,
  Tooltip,
  Collapse,
  CircularProgress,
} from '@mui/material';
import {
  ContentCopy as CopyIcon,
  Download as DownloadIcon,
  ArrowBack as ArrowBackIcon,
  CloudUpload as CloudUploadIcon,
  Edit as EditIcon,
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import api from '../services/api';

const MOCK_PIPELINE = {
  name: 'MySQL to Kafka',
  sourceConnector: {
    name: 'mysql-connector',
    type: 'mysql',
    connectorClass: 'io.debezium.connector.mysql.MySqlConnector',
    config: {
      'connector.class': 'io.debezium.connector.mysql.MySqlConnector',
      'database.hostname': 'localhost',
      'database.port': '3306',
      'database.user': 'dbuser',
      'database.password': '********',
      'database.server.name': 'dbserver1',
      'database.include.list': 'mydb',
      'table.include.list': 'mydb.orders,mydb.customers',
      'snapshot.mode': 'initial',
      'topic.prefix': 'dbserver1',
    },
    transforms: [
      { name: 'unwrap', type: 'io.debezium.transforms.ExtractNewRecordState', config: { 'drop.tombstones': 'false', 'delete.handling.mode': 'rewrite' } },
    ],
  },
};

function PipelinePreview() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [tab, setTab] = useState(0);
  const [copied, setCopied] = useState(false);
  const [validateResult, setValidateResult] = useState(null);
  const [validating, setValidating] = useState(false);

  const pipeline = MOCK_PIPELINE;

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const validatePipeline = async () => {
    setValidating(true);
    try {
      const response = await api.post('/pipelines/validate', pipeline);
      setValidateResult(response.data);
    } catch (err) {
      setValidateResult({ valid: false, errors: [{ field: 'api', message: 'Validation endpoint unavailable' }] });
    } finally {
      setValidating(false);
    }
  };

  const downloadConfig = (format) => {
    let content = '';
    let filename = `pipeline-${id || 'default'}`;
    if (format === 'json') {
      content = JSON.stringify(pipeline, null, 2);
      filename += '.json';
    } else if (format === 'yaml') {
      content = yamlStringify(pipeline);
      filename += '.yaml';
    }
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  };

  const yamlStringify = (obj, indent = 0) => {
    let result = '';
    const prefix = '  '.repeat(indent);
    for (const [key, value] of Object.entries(obj)) {
      if (value === null || value === undefined) continue;
      if (typeof value === 'object' && !Array.isArray(value)) {
        result += `${prefix}${key}:\n${yamlStringify(value, indent + 1)}`;
      } else if (Array.isArray(value)) {
        result += `${prefix}${key}:\n`;
        for (const item of value) {
          if (typeof item === 'object') {
            result += `${prefix}  - \n${yamlStringify(item, indent + 2)}`;
          } else {
            result += `${prefix}  - ${item}\n`;
          }
        }
      } else {
        result += `${prefix}${key}: ${value}\n`;
      }
    }
    return result;
  };

  const renderConfigJson = () => {
    const configLines = JSON.stringify(JSON.parse(JSON.stringify(pipeline.sourceConnector.config, (key, val) => key.includes('password') ? '********' : val)), null, 2);
    return (
      <Box>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">Connector Configuration</Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button size="small" startIcon={<CopyIcon />} onClick={() => copyToClipboard(configLines)} variant="outlined">
              {copied ? 'Copied!' : 'Copy'}
            </Button>
            <Button size="small" startIcon={<DownloadIcon />} onClick={() => downloadConfig('json')} variant="outlined">Download JSON</Button>
            <Button size="small" startIcon={<DownloadIcon />} onClick={() => downloadConfig('yaml')} variant="outlined">Download YAML</Button>
          </Box>
        </Box>
        <Paper
          variant="outlined"
          sx={{
            p: 2,
            backgroundColor: '#1e1e2e',
            color: '#cdd6f4',
            fontFamily: 'JetBrains Mono, Fira Code, Consolas, monospace',
            fontSize: 13,
            lineHeight: 1.6,
            borderRadius: 2,
            overflow: 'auto',
            maxHeight: 600,
          }}
        >
          <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{configLines}</pre>
        </Paper>
      </Box>
    );
  };

  const renderKafkaConnectJson = () => {
    const connectorPayload = {
      name: pipeline.sourceConnector.name,
      config: {
        ...pipeline.sourceConnector.config,
        'transforms': pipeline.sourceConnector.transforms?.map(t => t.name).join(','),
        ...pipeline.sourceConnector.transforms?.reduce((acc, t) => ({
          ...acc,
          [`transforms.${t.name}.type`]: t.type,
          ...Object.entries(t.config).reduce((a, [k, v]) => ({ ...a, [`transforms.${t.name}.${k}`]: v }), {}),
        }), {}),
      },
    };
    const payload = JSON.stringify(connectorPayload, null, 2);
    return (
      <Box>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">Kafka Connect REST Payload</Typography>
          <Button size="small" startIcon={<CopyIcon />} onClick={() => copyToClipboard(payload)} variant="outlined">
            {copied ? 'Copied!' : 'Copy'}
          </Button>
        </Box>
        <Paper
          variant="outlined"
          sx={{
            p: 2,
            backgroundColor: '#1e1e2e',
            color: '#cdd6f4',
            fontFamily: 'JetBrains Mono, Fira Code, Consolas, monospace',
            fontSize: 13,
            lineHeight: 1.6,
            borderRadius: 2,
            overflow: 'auto',
            maxHeight: 600,
          }}
        >
          <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{payload}</pre>
        </Paper>
        <Box sx={{ mt: 2 }}>
          <Typography variant="subtitle2" gutterBottom>Deploy via REST</Typography>
          <Paper sx={{ p: 2, backgroundColor: '#f5f5f5', fontFamily: 'monospace', fontSize: 13 }}>
            curl -X POST http://localhost:8083/connectors \<br/>
            &nbsp;&nbsp;-H "Content-Type: application/json" \<br/>
            &nbsp;&nbsp;-d '{connectorPayload.name}'
          </Paper>
        </Box>
      </Box>
    );
  };

  const renderSMTs = () => (
    <Box>
      <Typography variant="h6" gutterBottom>Transformations (SMT Chain)</Typography>
      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>#</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Configuration</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {pipeline.sourceConnector.transforms?.map((t, i) => (
              <TableRow key={t.name}>
                <TableCell>{i + 1}</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>{t.name}</TableCell>
                <TableCell><Chip label={t.type.split('.').pop()} size="small" variant="outlined" color="primary" /></TableCell>
                <TableCell>
                  <pre style={{ margin: 0, fontSize: 12 }}>{JSON.stringify(t.config, null, 2)}</pre>
                </TableCell>
              </TableRow>
            )) || (
              <TableRow>
                <TableCell colSpan={4} align="center">No transformations configured</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );

  const renderValidation = () => (
    <Box>
      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        <Button
          variant="contained"
          onClick={validatePipeline}
          disabled={validating}
          startIcon={validating ? <CircularProgress size={20} /> : <CheckCircleIcon />}
        >
          {validating ? 'Validating...' : 'Validate Pipeline'}
        </Button>
      </Box>
      {validateResult && (
        <Box>
          <Alert severity={validateResult.valid ? 'success' : 'error'} sx={{ mb: 3 }}>
            <AlertTitle>{validateResult.valid ? 'Pipeline Valid' : 'Validation Failed'}</AlertTitle>
          </Alert>
          {!validateResult.valid && validateResult.errors?.length > 0 && (
            <TableContainer component={Paper} variant="outlined">
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Field</TableCell>
                    <TableCell>Error</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {validateResult.errors?.map((err, i) => (
                    <TableRow key={i}>
                      <TableCell sx={{ fontWeight: 600 }}>{err.field}</TableCell>
                      <TableCell>{err.message}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </Box>
      )}
    </Box>
  );

  const renderDeploymentArtifacts = () => (
    <Box>
      <Typography variant="h6" gutterBottom>Generated Artifacts</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>The following deployment artifacts have been generated for your pipeline.</Typography>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Box>
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Strimzi KafkaConnector</Typography>
                  <Typography variant="body2" color="text.secondary">Custom Resource for Kubernetes deployment via Strimzi Operator</Typography>
                </Box>
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button size="small" variant="outlined" startIcon={<CopyIcon />}>Copy</Button>
                  <Button size="small" variant="outlined" startIcon={<DownloadIcon />}>Download</Button>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Box>
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Docker Compose</Typography>
                  <Typography variant="body2" color="text.secondary">Local development setup with Kafka, Schema Registry, and Debezium Connect</Typography>
                </Box>
                <Button size="small" variant="outlined" startIcon={<DownloadIcon />}>Download</Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Box>
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Helm Values</Typography>
                  <Typography variant="body2" color="text.secondary">Helm chart values for Strimzi Kafka and Debezium Connect</Typography>
                </Box>
                <Button size="small" variant="outlined" startIcon={<DownloadIcon />}>Download</Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Box>
                  <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Terraform</Typography>
                  <Typography variant="body2" color="text.secondary">Infrastructure as code for deploying the pipeline to Kubernetes</Typography>
                </Box>
                <Button size="small" variant="outlined" startIcon={<DownloadIcon />}>Download</Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );

  return (
    <Box sx={{ maxWidth: '1000px', mx: 'auto' }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
        <IconButton onClick={() => navigate(-1)}><ArrowBackIcon /></IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>{pipeline.name}</Typography>
          <Typography variant="body2" color="text.secondary">Review generated configuration and deployment artifacts</Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            startIcon={<EditIcon />}
            onClick={() => navigate(`/pipelines/${id || '1'}/mapping`)}
          >
            Edit Mapping
          </Button>
          <Button
            variant="contained"
            startIcon={<CloudUploadIcon />}
            onClick={() => navigate(`/pipelines/${id || '1'}/deploy`)}
          >
            Deploy
          </Button>
        </Box>
      </Box>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tab} onChange={(e, v) => setTab(v)}>
          <Tab label="Connector Config" />
          <Tab label="Kafka Connect API" />
          <Tab label="Transformations" />
          <Tab label="Validation" />
          <Tab label="Deployment Artifacts" />
        </Tabs>
      </Box>

      {tab === 0 && renderConfigJson()}
      {tab === 1 && renderKafkaConnectJson()}
      {tab === 2 && renderSMTs()}
      {tab === 3 && renderValidation()}
      {tab === 4 && renderDeploymentArtifacts()}
    </Box>
  );
}

export default PipelinePreview;